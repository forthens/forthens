#!/bin/bash
# 鸭科夫游戏启动脚本

echo "正在编译游戏..."
javac *.java

if [ $? -eq 0 ]; then
    echo "编译成功！正在启动游戏..."
    java DuckGame
else
    echo "编译失败，请检查代码错误。"
fi