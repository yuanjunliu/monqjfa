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

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import monq.jfa.actions.Copy;
import monq.jfa.actions.Drop;

/**
 *
 * @author &copy; 2004 Harald Kirsch
 */
public class DfaTest  {
  private static class Xaction extends AbstractFaAction {
    private String name;
    private Xaction(String name) { this.name = name;}
    @Override
    public void invoke(StringBuilder s, int start, DfaRun r) {}
    @Override
    public String toString() {return name;}
  }
  /**********************************************************************/
  @Test
  public void test_publicmatch() throws Exception {
    Dfa dfa =
      new Nfa("a+", new Xaction("xxx"))
      .compile(DfaRun.UNMATCHED_DROP);
    StringBuilder sb = new StringBuilder();

    CharSource cs = new CharSequenceCharSource("aaaaa");
    TextStore ts = null;
    assertEquals("xxx", dfa.match(cs, sb, ts).toString());
    assertEquals(5, sb.length());

    cs = new CharSequenceCharSource("baaaa");
    sb.setLength(0);
    assertNull(dfa.match(cs, sb, ts));
    assertEquals(0, sb.length());

    dfa =
      new Nfa("a(!b+)c", new Xaction("xxx"))
      .or("a(!XX|YY)(!z+)", new Xaction("yyy"))
      .compile(DfaRun.UNMATCHED_DROP);

    ts = new TextStore();
    cs = new CharSequenceCharSource("abbbcd");
    assertEquals("xxx", dfa.match(cs, sb, ts).toString());
    assertEquals("abbbc", ts.getPart(0));
    assertEquals("bbb", ts.getPart(1));

    cs = new CharSequenceCharSource("aXXzzz");
    assertEquals("yyy", dfa.match(cs, sb, ts).toString());
    assertEquals("aXXzzz", ts.getPart(0));
    assertEquals("XX", ts.getPart(1));
    assertEquals("zzz", ts.getPart(2));


    cs = new CharSequenceCharSource("aYYzzzz");
    assertEquals("yyy", dfa.match(cs, sb, ts).toString());
    assertEquals("aYYzzzz", ts.getPart(0));
    assertEquals("YY", ts.getPart(1));
    assertEquals("zzzz", ts.getPart(2));

  }

  @Test
  public void toNfa() throws Exception {
    Nfa nfa = new Nfa();
    nfa.or("a*", Copy.COPY);
    nfa.or("b+", Drop.DROP);

    Dfa dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
    nfa = dfa.toNfa();

    dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
    nfa = dfa.toNfa();

    dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
    StringBuilder out = new StringBuilder(10);
    dfa.match(new CharSequenceCharSource("c"), out, new SubmatchData());
    assertEquals(0, out.length());
    dfa.match(new CharSequenceCharSource("bbb"), out, new SubmatchData());
    assertEquals("bbb", out.toString());
  }

  /**
   * Dfa.toNfa() did not preserve the condition that the start state of an
   * Nfa may not have incoming transitions. In the example, this lead to "c*"
   * matching "xxxccc".
   */
  @Test
  public void toNfaMissingStartBug() throws Exception {
    Nfa nfa = new Nfa();
    nfa.or(".*b", Copy.COPY);
    Dfa dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
    nfa = dfa.toNfa();
    nfa.or("c+", Drop.DROP);

    //PrintStream p = new PrintStream("/home/harald/tmp/bla.dot");
    //nfa.toDot(p);
    //p.close();

    // this was matched by the buggy code
    dfa = nfa.compile(DfaRun.UNMATCHED_DROP);
    CharSource cs = new CharSequenceCharSource("xxxxccc");
    StringBuilder out = new StringBuilder();
    FaAction a = dfa.match(cs, out, (TextStore)null);
    assertNull(a);
    
    // verify the two branches of the Nfa, first ".*b"
    cs = new CharSequenceCharSource("ccccbbxb012");
    out.setLength(0);
    a = dfa.match(cs, out, (TextStore)null);
    assertEquals(Copy.COPY, a);
    assertEquals("ccccbbxb", out.toString());
    
    // now "c+"
    cs = new CharSequenceCharSource("cc");
    out.setLength(0);
    a = dfa.match(cs, out, (TextStore)null);
    assertEquals(Drop.DROP, a);
    assertEquals("cc", out.toString());
}

  @Test
  public void bug_r1024_stackOverflowOnDeepToNfa() throws Exception {
    final int SIZE = 200_000;
    char[] chars = new char[SIZE];
    Arrays.fill(chars, 'a');
    String longWord = new String(chars);
    Nfa nfa = new Nfa(longWord, Copy.COPY);
    Dfa dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
    nfa = dfa.toNfa();
    dfa = nfa.compile(DfaRun.UNMATCHED_COPY);

    StringBuilder out = new StringBuilder(SIZE);
    dfa.match(new CharSequenceCharSource(longWord), out, new SubmatchData());
    assertEquals(SIZE, out.length());
  }
}
