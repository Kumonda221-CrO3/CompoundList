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
		this.tail = this.head = new RegionNode(capacity);
	}
	
	public int getOption()
	{
		return option;
	}
	
	public void setOption(int option)
	{
		this.option = option;
	}
	
	public int size()
	{
		return size;
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
		LinkedNode preoperating = this.tail;
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
	
	public boolean add(int index, T element)
	{
		insert(index, element);
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
	
	void redirect(int index, LocatingResult result)
	{
		int expected = expectedLocation(index);
		if(expected == 0)
			return;
		
		int current = result.nodeIndex;
		
		if(current > (expected << 6))
		{
			// merge
			int ptr = result.base - 1;
			Object[] newRef = new Object[result.base];
			LinkedNode operating = result.result.prev;
			
			do {
				int size = operating.size();
				for(int i = 0; i < size; i++)
					newRef[ptr--] = operating.get(size - i - 1);
			} while((operating = operating.prev) != null);
			
			result.result.prev = null;
			
			RegionNode merged = new RegionNode();
			merged.ptr = newRef.length;
			merged.ref = newRef;
			merged.start = 0;
			merged.end = newRef.length - 1;
			merged.size = merged.capacity();
			merged.metacode |= METACODE_SIGNAL_FILLED;
			merged.linkBefore(result.result);
			this.head = merged;
			
			result.nodeIndex = 1;
		}
	}
	
	int expectedLocation(int index)
	{
		if(index < initCapacity)
			return 0;
		return (int) Math.ceil(Math.log((double)(index + 1) / (double)(initCapacity)) / GROWTH_LOG_CONSTANT);
	}
	
	// [...] | [index, ...]
	FracturingResult fracture(int index)
	{
		LocatingResult result = locate(index);
		LinkedNode operating = result.result;
		
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
		second.size = first.capacity() - ptr;
		second.checkFilled();
		second.linkAfter(first);
		first.end = ptr - 1;
		first.size = first.capacity();
		
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
		if(ptr > 0)
		{
			if((ptr + 1) < operating.capacity())
			{
				branch = new RegionNode();
				branch.ref = operating.ref;
				branch.end = operating.end;
				branch.start = operating.start + ptr + 1;
				branch.ptr = operating.ptr - ptr - 1;
				branch.size = operating.capacity() - ptr - 1;
				branch.checkFilled();
				branch.linkAfter(operating);
				operating.metacode |= METACODE_SIGNAL_FILLED;
			}
			operating.end = operating.start + ptr - 1;
			operating.size = operating.capacity();
		}
		else
		{
			operating.start++;
			operating.size--;
		}
		
		size--;
		return old;
	}
	
	RegionNode grow(LinkedNode operating)
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
		LinkedNode operating;
		if(!(index < this.size))
			throw new IndexOutOfBoundsException(index + " to size of " + this.size);
		
		operating = this.head;
		
		int counted = 0;
		int nodeIndex = 0;
		int size;
		while(!(((size = operating.size()) + counted) > index))
		{
			counted += size;
			operating = operating.next;
			nodeIndex++;
		}
		
		LocatingResult result = new LocatingResult(operating, counted, nodeIndex);
		
		redirect(index, result);
		
		return result;
	}
	
	public void clear()
	{
		this.capacity = 0;
		this.tail = this.head = new RegionNode(this.initCapacity);
		this.size = 0;
	}
	
	class RegionNode extends LinkedNode
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
		public int capacity()
		{
			return end - start + 1;
		}
		
		@Override
		public int size()
		{
			checkFilled();
			if((metacode & METACODE_SIGNAL_FILLED) != 0)
				return capacity();
			return size;
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
			size++;
			checkFilled();
		}
		
		void checkFilled()
		{
			if(!(size < capacity()))
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
		
		int size;
		
		Object[] ref;
	}
	
	class SingleNode extends LinkedNode
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
		LocatingResult(LinkedNode result, int base, int nodeIndex)
		{
			this.result = result;
			this.base = base;
			this.nodeIndex = nodeIndex;
		}
		
		LinkedNode result;
		
		int base;
		
		int nodeIndex;
	}
	
	class FracturingResult
	{
		FracturingResult(LinkedNode first, LinkedNode second)
		{
			this.first = first;
			this.second = second;
		}
		
		LinkedNode first;
		
		LinkedNode second;
	}
	
	abstract class LinkedNode implements Sized, MetaCode {
		public void remove()
		{
			if(prev == null)
				LinkedRegionList.this.head = next;
			else
			{
				prev.next = next;
				prev = null;
			}
			
			if(next == null)
				LinkedRegionList.this.tail = prev;
			else
			{
				next.prev = prev;
				next = null;
			}
		}
		
		public void linkBefore(LinkedNode node)
		{
			LinkedNode prev = node.prev;
			node.prev = this;
			if(prev != null)
				prev.next = this;
			this.next = node;
			this.prev = prev;
			if(prev == null)
				LinkedRegionList.this.head = this;
		}
		
		public void linkAfter(LinkedNode node)
		{
			LinkedNode next = node.next;
			node.next = this;
			if(next != null)
				next.prev = this;
			this.prev = node;
			this.next = next;
			if(next == null)
				LinkedRegionList.this.tail = this;
		}
		
		public abstract T get(int index);
		
		public int size()
		{
			return 1;
		}
		
		public int capacity()
		{
			return 1;
		}
		
		protected LinkedNode prev;
		
		protected LinkedNode next;
	}
	
	LinkedNode head;
	
	LinkedNode tail;
	
	private int size;
	
	private int capacity;
	
	int option;
	
	private final int initCapacity;
	
	private static final int DEFAULT_CAPACITY = 10;
	
	private static final double GROWTH_LOG_CONSTANT = Math.log(3D / 2D);
	
	// metacode type
	static final int METACODE_TYPE_REGION = 0x00000000;
	
	static final int METACODE_TYPE_SINGLE = 0x00000001;
	
	static final int METACODE_MASK_TYPE = 0x0000000F;
	
	// metacode signal
	static final int METACODE_SIGNAL_FILLED = 0x00000010;
	
	static final int METACODE_MASK_SIGNAL = 0x00000FF0;
	
	// options
	public static final int OPTION_STATIC_GROWTH = 0x00000010;
}