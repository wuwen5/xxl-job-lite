<template>
  <div class="help-container">
    <el-card>
      <template #header>
        <span>{{ t('menu.help') }}</span>
      </template>
      
      <div class="help-content">
        <h2>XXL-JOB 使用教程</h2>
        
        <el-collapse v-model="activeNames">
          <el-collapse-item title="1. 概述" name="1">
            <p>XXL-JOB 是一个分布式任务调度平台，其核心设计目标是开发迅速、学习简单、轻量级、易扩展。现已开放源代码并接入多家公司线上产品线，开箱即用。</p>
          </el-collapse-item>
          
          <el-collapse-item title="2. 快速入门" name="2">
            <h4>2.1 配置部署"调度中心"</h4>
            <p>调度中心项目: xxl-job-admin, 作用: 统一管理任务调度平台上调度任务, 负责触发调度执行。</p>
          </el-collapse-item>
          
          <el-collapse-item title="3. 任务管理" name="3">
            <h4>3.1 配置执行器</h4>
            <p>进入 "执行器管理" 界面, 新增执行器。</p>
            
            <h4>3.2 新建任务</h4>
            <p>进入 "任务管理" 界面, 新增任务。</p>
            
            <h4>3.3 触发执行</h4>
            <p>支持手动触发和自动调度。</p>
          </el-collapse-item>
          
          <el-collapse-item title="4. 执行器开发" name="4">
            <h4>4.1 引入依赖</h4>
            <pre>&lt;dependency&gt;
    &lt;groupId&gt;io.github.wuwen5.xxl-job&lt;/groupId&gt;
    &lt;artifactId&gt;xxl-job-core&lt;/artifactId&gt;
    &lt;version&gt;${project.version}&lt;/version&gt;
&lt;/dependency&gt;</pre>
            
            <h4>4.2 执行器配置</h4>
            <p>配置执行器参数, 包括调度中心地址、执行器端口等。</p>
          </el-collapse-item>
          
          <el-collapse-item title="5. 高级功能" name="5">
            <h4>5.1 路由策略</h4>
            <p>执行器集群部署时, 提供丰富的路由策略。</p>
            
            <h4>5.2 故障转移</h4>
            <p>调度失败时, 支持自动故障转移。</p>
            
            <h4>5.3 阻塞策略</h4>
            <p>任务执行时, 支持多种阻塞处理策略。</p>
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const activeNames = ref(['1'])
</script>

<style scoped lang="scss">
.help-container {
  .help-content {
    h2 {
      text-align: center;
      margin-bottom: 30px;
      color: #303133;
    }
    
    h4 {
      margin: 15px 0 10px;
      color: #409eff;
    }
    
    p {
      line-height: 1.8;
      color: #606266;
      margin: 5px 0;
    }
    
    pre {
      background-color: #f5f7fa;
      padding: 15px;
      border-radius: 4px;
      overflow-x: auto;
      font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    }
  }
}
</style>
