package cloud.apposs.netkit;

import cloud.apposs.netkit.rxio.actor.Actor;
import cloud.apposs.netkit.rxio.actor.ActorLock;
import cloud.apposs.netkit.rxio.actor.ActorTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestActor {
    public static void main(String[] args) throws InterruptedException {
        Actor actor = new Actor(4, false);
        // 第一个用户
        ActorLock lockKey = Actor.createLock(854);
        List<Thread> orderTheadList = new ArrayList<Thread>();
        for (int i = 0; i < 10; i++) {
            final int index = i;
            orderTheadList.add(new Thread(new Runnable() {
                @Override
                public void run() {
                    // 模拟多线程获取锁请求进来
                    actor.lock(lockKey, new MyTask(index, lockKey));
                }
            }));
        }
        for (int i = 0; i < orderTheadList.size(); i++) {
            orderTheadList.get(i).start();
            Thread.sleep(10);
        }

        // 第二个用户
        ActorLock lockKey2 = Actor.createLock(855);
        List<Thread> orderTheadList2 = new ArrayList<Thread>();
        for (int i = 0; i < 10; i++) {
            final int index = i;
            orderTheadList2.add(new Thread(new Runnable() {
                @Override
                public void run() {
                    // 模拟多线程获取锁请求进来
                    actor.lock(lockKey2, new MyTask(index, lockKey2));
                }
            }));
        }
        for (int i = 0; i < orderTheadList2.size(); i++) {
            orderTheadList2.get(i).start();
            Thread.sleep(10);
        }
    }

    static class MyTask implements ActorTask {
        private final int index;

        private final ActorLock lock;

        MyTask(int index, ActorLock lock) {
            this.index = index;
            this.lock = lock;
        }

        @Override
        public ActorLock getLockKey() {
            return lock;
        }

        @Override
        public void run() {
            Random random = new Random();
            try {
                int time = random.nextInt(2000);
                Thread.sleep(time);
                System.out.println(index + ";LockKey=" + getLockKey() + ";Sleep Time=" + time);
            } catch (InterruptedException e) {
            }
            lock.unlock();
        }
    }
}
