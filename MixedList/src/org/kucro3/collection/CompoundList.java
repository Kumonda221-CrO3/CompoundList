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
	
	public E get(int index)
	{
		return elementAt(index);
	}
	
	public E remove(int index)
	{
		return drop(locate(index));
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
		// TODO
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
			index = index - location.elementShift;
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
				
				window.end = window.start - index - 1;
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
		
		while((node.size() + elementShift) < index)
		{
			elementShift += node.size();
			nodeIndex++;
			node = node.next;
		}
		
		return new Location(elementShift, index - elementShift, nodeIndex, node);
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
			return (E) base[index];
		}
		
		@Override
		public void set(int index, E element)
		{
			base[index] = element;
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
			if(next != null)
			{
				next.prev = prev;
				next = null;
			}
			else
				tail = prev;
			
			if(prev != null)
			{
				prev.next = next;
				prev = null;
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
		Location(int shift, int index, int nodeIndex, Node located)
		{
			this.elementIndex = index;
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
		
		Node located;
	}
	
	class Fracture
	{
		Fracture(Window left, Window right)
		{
			this.left = left;
			this.right = right;
		}
		
		Window left;
		
		Window right;
	}
	
	public static final int DEFAULT_CAPACITY = 10;
}