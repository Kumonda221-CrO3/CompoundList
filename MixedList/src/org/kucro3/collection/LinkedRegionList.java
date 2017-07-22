package org.kucro3.collection;

@SuppressWarnings("unchecked")
public class LinkedRegionList<T> {
	public LinkedRegionList()
	{
		this(DEFAULT_CAPACITY);
	}
	
	public LinkedRegionList(int capacity, int option)
	{
		this(capacity);
		this.option = option;
	}
	
	public LinkedRegionList(int capacity)
	{
		if(capacity < 3)
			throw new IllegalArgumentException("Initializing capacity must be bigger than 2");
		this.initCapacity = capacity;
		this.head = new RegionNode(capacity);
	}
	
	public int getOption()
	{
		return option;
	}
	
	public void setOption(int option)
	{
		this.option = option;
	}
	
	public T get(int index)
	{
		return elementAt(index);
	}
	
	public T remove(int index)
	{
		return drop(index);
	}
	
	public boolean add(T element)
	{
		LinkedNode<T> preoperating = this.tail;
		if(preoperating == null)
			preoperating = this.head;
		
		RegionNode operating;
		if((preoperating.metacode() & METACODE_SIGNAL_FILLED) != 0)
			operating = grow(preoperating);
		else
			operating = (RegionNode) preoperating;
		
		operating.append(element);
		this.size++;
		
		return true;
	}
	
	public void insert(int index, T element)
	{
		FracturingResult result = fracture(index);
		SingleNode node = new SingleNode();
		node.element = element;
		node.linkBefore(result.second);
		size++;
	}
	
	
	
	// [...] | [index, ...]
	FracturingResult fracture(int index)
	{
		LocatingResult result = locate(index);
		LinkedNode<T> operating = result.result;
		
		if(operating.size() == 1)
			return new FracturingResult(operating, operating);
		
		int ptr = index - result.base;
		if(ptr == 0)
			return new FracturingResult(operating, operating);
		
		RegionNode first = (RegionNode) operating;
		RegionNode second = new RegionNode();
		
		second.start = ptr;
		second.end = first.end;
		second.ref = first.ref;
		second.ptr = first.ptr - ptr;
		second.checkFilled();
		second.linkAfter(first);
		first.end = ptr - 1;
		
		return new FracturingResult(first, second);
	}
	
	T drop(int index)
	{
		LocatingResult result = locate(index);
		int ptr = index - result.base;
		
		if(result.result.size() == 1)
		{
			result.result.remove();
			size--;
			return result.result.get(0);
		}
		
		RegionNode operating = (RegionNode) result.result;
		T old = operating.get(ptr);
		operating.set(ptr, null);
		
		RegionNode branch;
		if((this.option & OPTION_TIRM_ON_REMOVE) == 0)
		{
			if((ptr + 1) < operating.size())
			{
				branch = new RegionNode();
				branch.ref = operating.ref;
				branch.end = operating.end;
				branch.start = ptr + 1;
				branch.ptr = operating.ptr - ptr - 1;
				branch.checkFilled();
				branch.linkAfter(operating);
				operating.metacode |= METACODE_SIGNAL_FILLED;
			}
			operating.end = ptr - 1;
		}
		else
		{
			if((ptr + 1) < operating.size())
			{
				Object[] oldRef = operating.ref;
				Object[] newRef = new Object[ptr];
				Object[] newBranchRef = new Object[operating.size() - ptr - 1];
				
				System.arraycopy(oldRef, 0, newRef, 0, newRef.length);
				System.arraycopy(oldRef, ptr + 1, newBranchRef, 0, newBranchRef.length);
				
				branch = new RegionNode();
				branch.end = newBranchRef.length - 1;
				branch.ptr = operating.ptr - ptr - 1;
				branch.checkFilled();
				branch.linkAfter(operating);
				branch.ref = newBranchRef;
				
				operating.ref = newRef;
				operating.end = ptr - 1;
				operating.metacode |= METACODE_SIGNAL_FILLED;
				
				this.capacity--;
			}
			else
			{
				operating.end--;
				Object[] newRef = new Object[operating.ref.length - 1];
				System.arraycopy(operating.ref, 0, newRef, 0, newRef.length);
				operating.ref = newRef;
			}
		}
		
		size--;
		return old;
	}
	
	RegionNode grow(LinkedNode<T> operating)
	{
		RegionNode outcome = new RegionNode(
				(option & OPTION_STATIC_GROWTH) == 0 ? this.capacity >> 1 : this.initCapacity
			);
		outcome.linkAfter(operating);
		this.tail = outcome;
		return outcome;
	}
	
	T elementAt(int index)
	{
		LocatingResult result = locate(index);
		return result.result.get(index - result.base);
	}
	
	LocatingResult locate(int index)
	{
		LinkedNode<T> operating;
		if(!(index < this.size))
			throw new IndexOutOfBoundsException(index + " to size of " + this.size);
		
		operating = this.head;
		
		int counted = 0;
		int size;
		while(!(((size = operating.size()) + counted) > index))
		{
			counted += size;
			operating = operating.next;
		}
		
		return new LocatingResult(operating, counted);
	}
	
	public void clear()
	{
		this.head = new RegionNode(this.initCapacity);
		this.tail = null;
		this.size = 0;
	}
	
	class RegionNode extends LinkedNode<T>
	{
		RegionNode()
		{
		}
		
		RegionNode(int capacity)
		{
			this.ref = new Object[capacity];
			this.start = 0;
			this.end = capacity - 1;
			LinkedRegionList.this.capacity += capacity;
		}
		
		@Override
		public int size()
		{
			return end - start + 1;
		}
		
		@Override
		public int metacode()
		{
			return metacode;
		}
		
		@Override
		public T get(int index)
		{
			return (T) (ref[start + index]);
		}
		
		void append(T element)
		{
			ref[start + ptr++] = element;
			checkFilled();
		}
		
		void checkFilled()
		{
			if(!(ptr < size()))
				metacode |= METACODE_SIGNAL_FILLED;
		}
		
		void set(int index, T element)
		{
			ref[start + index] = element;
		}
		
		int metacode = METACODE_TYPE_REGION;
		
		int start;
		
		int end;
		
		int ptr;
		
		Object[] ref;
	}
	
	class SingleNode extends LinkedNode<T>
	{
		SingleNode()
		{
			LinkedRegionList.this.capacity++;
		}
		
		@Override
		public T get(int index)
		{
			return element;
		}
		
		@Override
		public int metacode()
		{
			return metacode;
		}
		
		int metacode = METACODE_TYPE_SINGLE | METACODE_SIGNAL_FILLED;
		
		T element;
	}
	
	class LocatingResult
	{
		LocatingResult(LinkedNode<T> result, int base)
		{
			this.result = result;
			this.base = base;
		}
		
		LinkedNode<T> result;
		
		int base;
	}
	
	class FracturingResult
	{
		FracturingResult(LinkedNode<T> first, LinkedNode<T> second)
		{
			this.first = first;
			this.second = second;
		}
		
		LinkedNode<T> first;
		
		LinkedNode<T> second;
	}
	
	LinkedNode<T> head;
	
	LinkedNode<T> tail;
	
	private int size;
	
	private int capacity;
	
	int option;
	
	private final int initCapacity;
	
	private static final int DEFAULT_CAPACITY = 10;
	
	// metacode type
	static final int METACODE_TYPE_REGION = 0x00000000;
	
	static final int METACODE_TYPE_SINGLE = 0x00000001;
	
	static final int METACODE_MASK_TYPE = 0x0000000F;
	
	// metacode signal
	static final int METACODE_SIGNAL_FILLED = 0x00000010;
	
	static final int METACODE_SIGNAL_MARKED_AS_MEGRING_ROOT = 0x00000100;
	
	static final int METACODE_SIGNAL_CONNECTED_TO_MEGRING_ROOT = 0x00000200;
	
	static final int METACODE_MASK_SIGNAL = 0x00000FF0;
	
	// options
	public static final int OPTION_STATIC_GROWTH = 0x00000010;
	
	public static final int OPTION_TIRM_ON_REMOVE = 0x00000001;
}