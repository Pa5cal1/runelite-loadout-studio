/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import java.util.function.Consumer;
/** Bank scripts may be nested. Observe their pre-event and finish once outside the VM. */
final class BankBuildRefresh {
    private final Consumer<Runnable> queue;private final Consumer<Boolean> refresh;
    private boolean queued,reopen;
    BankBuildRefresh(Consumer<Runnable> queue,Consumer<Boolean> refresh){this.queue=queue;this.refresh=refresh;}
    void request(boolean opening){
        reopen|=opening;if(queued)return;queued=true;
        queue.accept(()->{boolean openingBank=reopen;queued=false;reopen=false;refresh.accept(openingBank);});
    }
}
