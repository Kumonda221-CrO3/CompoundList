package org.kucro3.collection;

@SuppressWarnings("unchecked")
public class LinkedRegionList<T> {
	public LinkedRegionList()
	{
		this(DEFAULT_CAPACITY);
	}
	
	public LinkedRegionList(int capacity)
	{
		if(capacity < 3)
			throw new IllegalArgumentException("Initializing capacity must be bigger than 2");
		this.initCapacity = capacity;
		this.head = new RegionNode(capacity);
	}
	
	public T get(int index)
	{
		return elementAt(index);
	}
	
	public <E> boolean add(T element)
	{
		assert this.head != null : "Zero head"; // TODO
		
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
	
	RegionNode grow(LinkedNode<T> operating)
	{
		RegionNode outcome = new RegionNode(this.capacity >> 1);
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
			return (T) ref[index];
		}
		
		void append(T element)
		{
			assert (metacode & METACODE_SIGNAL_FILLED) == 0 : "Overflow"; // TODO
			
			ref[start + ptr++] = element;
			if(!(ptr < size()))
				metacode |= METACODE_SIGNAL_FILLED;
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
			assert index == 0 : "Not indexable";
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
	
	private class LocatingResult
	{
		LocatingResult(LinkedNode<T> result, int base)
		{
			this.result = result;
			this.base = base;
		}
		
		LinkedNode<T> result;
		
		int base;
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
	
	static final int METACODE_MASK_SIGNAL = 0x00000FF0;
	
	public static final int OPTION_TIRM_ON_REMOVE = 0x00000001;
}