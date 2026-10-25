/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import java.util.*;
import java.util.function.IntUnaryOperator;
/** Positions refer to the original child index; no item data is rewritten. */
final class BankViewPlan {
    /** Duplicate visual cells show real observed stock, never a fabricated stack. */
    static Map<Integer,Integer> stock(int[] ids,int[] quantities,IntUnaryOperator canonical){
        Map<Integer,Integer> result=new HashMap<>();for(int i=0;i<ids.length;i++)if(ids[i]>0)result.merge(canonical.applyAsInt(ids[i]),Math.max(0,quantities[i]),Math::max);return result;
    }
    static int[] positions(int[] ids,int[] layout,IntUnaryOperator canonical){int[] result=new int[ids.length];Arrays.fill(result,-1);Set<Integer> occupied=new HashSet<>();int next=layout.length;
        for(int source=0;source<ids.length;source++){if(ids[source]<1)continue;int id=canonical.applyAsInt(ids[source]),pos=-1;for(int i=0;i<layout.length;i++)if(layout[i]>0&&canonical.applyAsInt(layout[i])==id&&!occupied.contains(i)){pos=i;break;}if(pos<0){while(occupied.contains(next))next++;pos=next++;}occupied.add(pos);result[source]=pos;}
        return result;
    }
    static void callback(String name,int[] stack,int size,java.util.function.IntPredicate matches){if(name.equals("bankSearchFilter")&&size>=2){int id=stack[size-1];stack[size-2]=id>0&&matches.test(id)?1:0;}else if((name.equals("bankBuildTab")||name.equals("getSearchingTagTab"))&&size>0)stack[size-1]=1;}
    /** Every unoccupied requested cell gets a visual-only item; holes remain empty. */
    static int[] placeholders(int[] layout,int[] occupiedPositions){
        int[] result=layout.clone();for(int position:occupiedPositions)if(position>=0&&position<result.length)result[position]=-1;return result;
    }
}
