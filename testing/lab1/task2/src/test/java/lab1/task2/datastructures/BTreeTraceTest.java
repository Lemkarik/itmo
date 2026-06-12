package lab1.task2.datastructures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BTreeTraceTest {

  private BTree tree;

  @BeforeEach
  public void setUp() {
    tree = new BTree();
    tree.clearTrace();
  }

  @Test
  @DisplayName("Trace: simple insert sequence")
  public void testInsertTraceSimple() {

    tree.insert(10);
    tree.insert(20);
    tree.insert(5);

    List<String> trace = tree.getTrace();

    assertTrue(trace.contains("INSERT_10"));
    assertTrue(trace.contains("INSERT_20"));
    assertTrue(trace.contains("INSERT_5"));

    int i10 = trace.indexOf("INSERT_10");
    int i20 = trace.indexOf("INSERT_20");
    int i5 = trace.indexOf("INSERT_5");

    assertTrue(i10 < i20);
    assertTrue(i20 < i5);
  }

  @Test
  @DisplayName("Trace: root split is triggered")
  public void testRootSplitTrace() {

    for (int i = 1; i <= 6; i++) {
      tree.insert(i);
    }

    List<String> trace = tree.getTrace();

    assertTrue(trace.contains("ROOT_SPLIT"));
    assertTrue(trace.contains("SPLIT_CHILD"));
  }

  @Test
  @DisplayName("Trace: delete triggers events")
  public void testDeleteTrace() {

    for (int i = 1; i <= 10; i++) {
      tree.insert(i);
    }

    tree.clearTrace();

    tree.delete(5);

    List<String> trace = tree.getTrace();

    assertTrue(trace.stream().anyMatch(s -> s.startsWith("DELETE_5")));
  }

  @Test
  @DisplayName("Trace is non-empty and records operations")
  public void testTraceNotEmpty() {

    tree.insert(100);
    tree.insert(50);

    assertFalse(tree.getTrace().isEmpty());
  }
}
