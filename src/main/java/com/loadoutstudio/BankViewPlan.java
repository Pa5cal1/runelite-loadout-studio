/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import java.util.*;
import java.util.function.IntUnaryOperator;
/** Positions refer to the original child index; no item data is rewritten. */
final class BankViewPlan {
    static int[] positions(int[] ids,int[] layout,IntUnaryOperator canonical){int[] result=new int[ids.length];Arrays.fill(result,-1);Set<Integer> occupied=new HashSet<>();int next=layout.length;
        for(int source=0;source<ids.length;source++){if(ids[source]<1)continue;int id=canonical.applyAsInt(ids[source]),pos=-1;for(int i=0;i<layout.length;i++)if(layout[i]>0&&canonical.applyAsInt(layout[i])==id&&!occupied.contains(i)){pos=i;break;}if(pos<0){while(occupied.contains(next))next++;pos=next++;}occupied.add(pos);result[source]=pos;}
        return result;
    }
    static void callback(String name,int[] stack,int size,java.util.function.IntPredicate matches){if(name.equals("bankSearchFilter")&&size>=2){int id=stack[size-1];stack[size-2]=id>0&&matches.test(id)?1:0;}else if((name.equals("bankBuildTab")||name.equals("getSearchingTagTab"))&&size>0)stack[size-1]=1;}
}
