package org.kucro3.collection;

import java.util.NoSuchElementException;

@SuppressWarnings("unchecked")
public class CompoundList<E> {
	public CompoundList()
	{
		this(DEFAULT_CAPACITY);
	}
	
	public CompoundList(int initCapacity)
	{
		this.initCapacity = initCapacity;
		this.totalCapacity = initCapacity;
		
		if(initCapacity < 2)
			throw new IllegalArgumentException("Initial capacity must be bigger than 2");
		
		Window init = new Window();
		init.base = new Object[initCapacity];
//		init.start = 0;
		init.end = initCapacity - 1;
		this.head = this.tail = init;
	}
	
	public int size()
	{
		return size;
	}
	
	public boolean isEmpty()
	{
		return size == 0;
	}
	
	public boolean add(E element)
	{
		append(element);
		return true;
	}
	
	public boolean add(int index, E element)
	{
		insert(index, element);
		return true;
	}
	
	public E get(int index)
	{
		return elementAt(index);
	}
	
	public E remove(int index)
	{
		return drop(locate(index));
	}
	
	void insert(int index, E element)
	{
		Fracture fracture = fracture(locate(index));
		Element e = new Element();
		
		e.element = element;
		e.linkBefore(fracture.right);
		
		size++;
	}
	
	void append(E element)
	{
		Window window;
		if(tail.isFilled())
			window = grow();
		else
			window = (Window) tail;
		
		window.append(element);
		window.size++;
		
		this.size++;
	}
	
	int expected(int nodeIndex)
	{
		if(nodeIndex < initCapacity)
			return 0;
		return (int) Math.ceil(Math.log((double)(nodeIndex + 1) / (double)(initCapacity)) / GROWTH_LOG_CONSTANT);
	}
	
	Location redirect(Location location)
	{
		int expected = expected(location.rawIndex);
		if(expected == 0)
			return location;
		
		int current = location.nodeIndex;
		
		if(current > (expected << 6)) // merge
		{
			int ptr = location.elementShift - 1;
			Object[] newBase = new Object[location.elementShift];
			Window window = new Window();
			Node operating = location.located.prev;
			
			do for(int i = 0; i < operating.size(); i++)
					newBase[ptr--] = operating.get(operating.size() - i - 1);
			while((operating = operating.prev) != null);
			
			location.located.prev.next = null;
			location.located.prev = null;
			
			window.base = newBase;
//			window.start = 0;
			window.end = newBase.length - 1;
			window.size = newBase.length;
			
			window.linkBefore(location.located);
			
			location.nodeIndex = 1;
		}
		
		return location;
	}
	
	Window grow()
	{
		Window growth = new Window();
		int size = this.totalCapacity >> 1;
		
		this.totalCapacity += size;
		
		growth.base = new Object[size];
//		growth.start = 0;
		growth.end = size - 1;
		
		growth.linkAfter(tail);
		
		return growth;
	}
	
	Fracture fracture(Location location)
	{
		if(location.elementIndex == 0)
			return new Fracture(location.located, location.located);
		else if(location.elementIndex + 1 == location.located.size())
		{
			Window window = (Window) location.located;
			Element next = new Element();
			
			next.element = location.element();
			
			window.end--;
			window.size--;
			
			next.linkAfter(window);
			
			return new Fracture(window, next);
		}
		else
		{
			Window window = (Window) location.located;
			Window next = new Window();
			
			next.base = window.base;
			next.start = window.start + location.elementIndex;
			next.end = window.end;
			next.size = window.size - location.elementIndex;
			next.ptr = window.ptr;
			
			window.end = next.start - 1;
			window.size = window.size - next.size;
			
			next.linkAfter(window);
			
			return new Fracture(window, next);
		}
	}
	
	E drop(Location location)
	{
		int index = location.elementIndex;
		Node node = location.located;
		E old;
		if(node.size() == 1)
		{
			old = node.get(0);
			node.remove();
		}
		else
		{
			old = node.get(index);
			
			Window window = (Window) node;
			
			window.set(index, null);
			if(index == 0)
			{
				window.start++;
				window.size--;
			}
			else if(index == (window.size() - 1))
			{
				window.end--;
				window.size--;
			}
			else
			{
				Window next = new Window();
				
				next.base = window.base;
				next.ptr = window.ptr;
				next.start = window.start + index + 1;
				next.end = window.end;
				next.size = window.size - index - 1;
				
				window.end = window.start + index - 1;
				window.size = window.size - next.size - 1;
				
				next.linkAfter(window);
			}
		}
		
		size--;
		
		return old;
	}
	
	E elementAt(int index)
	{
		return locate(index).element();
	}
	
	Location locate(int index)
	{
		if(isEmpty())
			throw new NoSuchElementException("Empty");
		
		if(index < size)
			;
		else
			throw new IndexOutOfBoundsException(index + " in size of " + size);
		
		int elementShift = 0, nodeIndex = 0;
		Node node = head;
		
		while(!((node.size() + elementShift) > index))
		{
			elementShift += node.size();
			nodeIndex++;
			node = node.next;
		}
		
		return redirect(new Location(elementShift, index, index - elementShift, nodeIndex, node));
	}
	
	final int initCapacity;
	
	int totalCapacity;
	
	int size;
	
	Node head;
	
	Node tail;
	
	class Window extends Node
	{
		@Override
		public boolean isFilled() 
		{
			return !(size < capacity());
		}

		@Override
		public int size() 
		{
			return size;
		}
		
		@Override
		public int capacity() 
		{
			return end - start + 1;
		}

		@Override
		public E get(int index)
		{
			return (E) base[start + index];
		}
		
		@Override
		public void set(int index, E element)
		{
			base[start + index] = element;
		}
		
		@Override
		public void remove()
		{
			base = null;
			super.remove();
		}
		
		void append(E element)
		{
			base[ptr++] = element;
		}
		
		Object[] base;
		
		int start;
		
		int end;
		
		int size;
		
		int ptr;
	}
	
	class Element extends Node
	{
		@Override
		public boolean isFilled()
		{
			return true;
		}

		@Override
		public int size() 
		{
			return 1;
		}

		@Override
		public int capacity() 
		{
			return 1;
		}

		@Override
		public E get(int index) 
		{
			return element;
		}
		
		@Override
		public void set(int index, E element)
		{
			this.element = element;
		}
		
		@Override
		public void remove()
		{
			element = null;
			super.remove();
		}
		
		E element;
	}
	
	abstract class Node
	{
		public void remove()
		{
			Node next = this.next;
			Node prev = this.prev;
			
			if(next != null)
			{
				next.prev = prev;
				this.next = null;
			}
			else
				tail = prev;
			
			if(prev != null)
			{
				prev.next = next;
				this.prev = null;
			}
			else
				head = next;
		}
		
		public void linkAfter(Node node)
		{
			if(node.next == null)
				tail = this;
			else
				node.next.prev = this;
			
			next = node.next;
			prev = node;
			node.next = this;
		}
		
		public void linkBefore(Node node)
		{
			if(node.prev == null)
				head = this;
			else
				node.prev.next = this;
			
			next = node;
			prev = node.prev;
			node.prev = this;
		}
		
		public abstract boolean isFilled();
		
		public abstract int size();
		
		public abstract int capacity();
		
		public abstract E get(int index);
		
		public abstract void set(int index, E element);
		
		Node next;
		
		Node prev;
	}
	
	class Location
	{
		Location(int shift, int rawIndex, int elementIndex, int nodeIndex, Node located)
		{
			this.rawIndex = rawIndex;
			this.elementIndex = elementIndex;
			this.elementShift = shift;
			this.nodeIndex = nodeIndex;
			this.located = located;
		}
		
		public E element()
		{
			return located.get(elementIndex);
		}
		
		public void element(E element)
		{
			located.set(elementIndex, element);
		}
		
		int elementShift;
		
		int elementIndex;
		
		int nodeIndex;
		
		int rawIndex;
		
		Node located;
	}
	
	class Fracture
	{
		Fracture(Node left, Node right)
		{
			this.left = left;
			this.right = right;
		}
		
		Node left;
		
		Node right;
	}
	
	public static final int DEFAULT_CAPACITY = 10;
	
	private static final double GROWTH_LOG_CONSTANT = Math.log(3D / 2D);
}