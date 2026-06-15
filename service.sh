#!/bin/bash
# 金木智库后端服务管理脚本：启动 / 停止 / 重启
# 用法: ./service.sh {start|stop|restart}

APP_NAME="ai-hedge-fund-java-1.0.0-SNAPSHOT.jar"
APP_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_PATH="$APP_DIR/$APP_NAME"
LOG_FILE="$APP_DIR/app.log"
PID_FILE="$APP_DIR/app.pid"

start() {
    if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
        echo "服务已在运行，PID: $(cat "$PID_FILE")"
        exit 1
    fi
    cd "$APP_DIR" || exit 1
    nohup java -jar "$JAR_PATH" > "$LOG_FILE" 2>&1 &
    echo $! > "$PID_FILE"
    echo "服务已启动，PID: $!"
}

stop() {
    if [ ! -f "$PID_FILE" ] || ! kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
        echo "服务未运行"
        rm -f "$PID_FILE"
        exit 1
    fi
    PID=$(cat "$PID_FILE")
    kill "$PID"
    rm -f "$PID_FILE"
    echo "服务已停止，PID: $PID"
}

restart() {
    stop
    sleep 2
    start
}

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    *)
        echo "用法: $0 {start|stop|restart}"
        exit 1
        ;;
esac
