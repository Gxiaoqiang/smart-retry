<template>
  <el-container style="height: 100vh;">
    <!-- 侧边栏 -->
    <el-aside width="200px">
      <div class="logo">
        <h2>Smart Retry</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>整体分布</span>
        </el-menu-item>
        <el-menu-item index="/instance">
          <el-icon><Monitor /></el-icon>
          <span>实例管理</span>
        </el-menu-item>
        <el-menu-item index="/task">
          <el-icon><List /></el-icon>
          <span>任务管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.el-aside {
  background-color: #304156;
  color: #fff;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b3a4c;
}

.logo h2 {
  color: #fff;
  font-size: 18px;
  margin: 0;
}

.el-menu {
  border-right: none;
}

.el-main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
