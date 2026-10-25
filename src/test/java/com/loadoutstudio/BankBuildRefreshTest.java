/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class BankBuildRefreshTest {
 @Test public void nestedBuildNeedsNoPostEventAndRunsAfterItsWidgetsAreReady(){
  ArrayDeque<Runnable> queue=new ArrayDeque<>();List<Boolean> refreshes=new ArrayList<>();boolean[] ready={false};BankBuildRefresh refresh=new BankBuildRefresh(queue::add,opening->{assertTrue(ready[0]);refreshes.add(opening);});
  refresh.request(false);assertTrue(refreshes.isEmpty());ready[0]=true;queue.remove().run();assertEquals(Collections.singletonList(false),refreshes);
 }
 @Test public void initAndRepeatedBuildsCoalesceWithoutLosingTheOpeningRequest(){
  ArrayDeque<Runnable> queue=new ArrayDeque<>();List<Boolean> refreshes=new ArrayList<>();BankBuildRefresh refresh=new BankBuildRefresh(queue::add,refreshes::add);refresh.request(false);refresh.request(true);refresh.request(false);assertEquals(1,queue.size());queue.remove().run();assertEquals(Collections.singletonList(true),refreshes);
 }
 @Test public void subsequentBuildCanRefreshAgainWithoutReopeningTheFilter(){
  ArrayDeque<Runnable> queue=new ArrayDeque<>();List<Boolean> refreshes=new ArrayList<>();BankBuildRefresh refresh=new BankBuildRefresh(queue::add,refreshes::add);refresh.request(true);queue.remove().run();refresh.request(false);queue.remove().run();assertEquals(Arrays.asList(true,false),refreshes);
 }
}
