/*+********************************************************************* 
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation
Foundation, Inc., 59 Temple Place - Suite 330, Boston MA 02111-1307, USA.
************************************************************************/

package monq.jfa;

import java.util.*;

/**
 * an implementation of <code>Set</code> based on a simple hashing
 * scheme.
 *
 * @author &copy; 2004 Harald Kirsch
 */
public class PlainSet<E> extends AbstractSet<E> {
  // As soon as size/elems.length>=loadFactorMax, we reallocate elems
  // such that we end up with loadFactor. Some experiments showed that
  // for my application the following combination was the best
  // tradeoff between speed and memory usage.
  private static final float loadFactorMax = 3.0F;
  private static final float loadFactor = 1.5F;
  private static final float ilF = 1.0F/loadFactor;
      
  private Elem<E>[] elems;
  private int size = 0;
  private int upperThreshold;
  /**********************************************************************/
  public PlainSet() {
    this(8);
  }
  /**********************************************************************/
  public PlainSet(Collection<E> col) {
    this(col.size());
    addAll(col);
  }
  /**********************************************************************/
  public PlainSet(int initialCapacity) {
    int s = 8;
    while( s<initialCapacity ) s = s<<1;
    allocate(s);
  }
  /**********************************************************************/
  public int size() { return size; }
  /**********************************************************************/
  @Override
  public boolean add(E o) {
    int idx = indexFor(o);
    if( oflowFind(elems[idx], o) ) return false;
    elems[idx] = new Elem<E>(o, elems[idx]);
    size += 1;
    if( size>=upperThreshold ) rearrange();
    return true;
  }
  /**********************************************************************/
  public boolean contains(Object o) {
    return oflowFind(elems[indexFor(o)], o);
  }
  /**********************************************************************/
  @Override
  public boolean addAll(Collection<? extends E> col) {
    if( !(col instanceof PlainSet) ) {
      return super.addAll(col);
    }
    @SuppressWarnings("unchecked")
    PlainSet<E> other = (PlainSet<E>)col;
    return addStraight(other.elems);
  }
  /**********************************************************************/
  public boolean remove(Object o) {
    int idx = indexFor(o);
    Elem<E> e = elems[idx];
    if( e==null ) return false;
    if( o.equals(e.value) ) {
      elems[idx] = e.next;
      size -= 1;
      return true;
    }

    while( e.next!=null ) {
      if( !o.equals(e.next.value) ) {
	e = e.next;
	continue;
      }
      e.next = e.next.next;
      size -= 1;
      return true;
    }
    return false;
  }
  /**********************************************************************/
  public Iterator<E> iterator() { return new PSiter(); }
  /**********************************************************************/
  private int indexFor(Object o) { 
    return (Integer.MAX_VALUE&o.hashCode())%elems.length; 
  }
  /**********************************************************************/
  private boolean oflowFind(Elem<E> list, Object o) {
    while( list!=null ) {
      if( o.equals(list.value) ) return true;;
      list = list.next;
    }
    return false;
  }
  /**********************************************************************/
  private boolean addStraight(Elem<E>[] ary) {
    int oldSize = size;
    for(int i=0; i<ary.length; i++) {
      for(Elem<E> e=ary[i]; e!=null; e=e.next) {
	int idx = indexFor(e.value);
	if( oflowFind(elems[idx], e.value) ) continue;
	elems[idx] = new Elem<>(e.value, elems[idx]);
	size += 1;
	if( size>upperThreshold ) rearrange();
      }
    }
    return oldSize!=size;
  }
  /**********************************************************************/
  private void allocate(int s) {
    @SuppressWarnings("unchecked")
    Elem<E>[] tmp = new Elem[s];
    elems = tmp;
    size = 0;
    upperThreshold = (int)(loadFactorMax*s);
  }
  /**********************************************************************/
  private void rearrange() {
    //System.out.print("rearrange: size="+size+", length="+elems.length);
    int s = (int)(size*ilF);
    if( s<4 ) s = 4;
    //System.out.println(", newLength="+s);

    Elem<E>[] oldElems = elems;
    allocate(s);
    addStraight(oldElems);
  }
  /**********************************************************************/
  private class PSiter implements Iterator<E> {
    private int i = 0;
    private Elem<E> e = null;
    private E current = null;
    public PSiter() {
      for(i=0; i<elems.length; i++) {
	if( elems[i]!=null ) {
	  e = elems[i];
	  return;
	}
      }
    }
    public boolean hasNext() { return e!=null; }
    public E next() {
      if( e==null ) throw new NoSuchElementException();
      current = e.value;
      //System.out.println("it.next: "+current);
      e = e.next;
      while( e==null && ++i<elems.length ) e = elems[i];
      return current;
    }
    public void remove() {
      if( current==null ) throw new IllegalStateException();
      PlainSet.this.remove(current);
      current = null;
    }
  }
  /**********************************************************************/
  private static class Elem<D> {
    public D value;
    public Elem<D> next = null;
    public Elem(D v, Elem<D> n) {
      this.value = v;
      this.next = n;
    }
  }
  /**********************************************************************/
}
