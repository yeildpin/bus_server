#! /bin/bash
nohup java -DdevelopmentMode=true -server -Xms3550M -Xmx3550M -Xmn1330M -Xrs -XX:+UseGCOverheadLimit -XX:+UseParallelGC -XX:ParallelGCThreads=8 -XX:+UseParallelOldGC -XX:MaxGCPauseMillis=100 -XX:+UseAdaptiveSizePolicy -XX:OnOutOfMemoryError="./startup.sh" -XX:OnError="./startup.sh" -Djava.ext.dirs=../lib/ com.bus.server.BusServer &
echo $! > ./app.pid
