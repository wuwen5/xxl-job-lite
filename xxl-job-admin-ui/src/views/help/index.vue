<template>
  <div class="help-container">
    <el-card>
      <template #header>
        <span>{{ t('menu.help') }}</span>
      </template>

      <div class="help-content">
        <h2>XXL-JOB 使用教程</h2>

        <el-collapse v-model="activeNames">
          <!-- 1. 概述 -->
          <el-collapse-item title="1. 概述" name="1">
            <p>
              XXL-JOB 是一个分布式任务调度平台，其核心设计目标是开发迅速、学习简单、轻量级、易扩展。现已开放源代码并接入多家公司线上产品线，开箱即用。
            </p>
            <ul>
              <li><strong>调度中心（xxl-job-admin）：</strong>统一管理任务调度，负责触发调度执行。</li>
              <li><strong>执行器（xxl-job-executor）：</strong>接收调度中心的调度请求，执行具体的业务逻辑。</li>
            </ul>
          </el-collapse-item>

          <!-- 2. 快速入门 -->
          <el-collapse-item title="2. 快速入门" name="2">
            <h4>2.1 部署调度中心</h4>
            <p>
              调度中心项目为 <code>xxl-job-admin</code>，负责统一管理任务调度平台上调度任务，负责触发调度执行。
            </p>
            <ol>
              <li>初始化数据库脚本（<code>doc/db/tables_xxl_job.sql</code>）。</li>
              <li>修改 <code>application.properties</code> 中的数据库连接信息。</li>
              <li>启动 <code>xxl-job-admin</code>，默认端口 <code>8080</code>，默认登录账号 <code>admin / 123456</code>。</li>
            </ol>

            <h4>2.2 配置执行器</h4>
            <p>进入"执行器管理"界面，新增执行器：</p>
            <ul>
              <li><strong>AppName：</strong>执行器唯一标识，与代码中配置对应。</li>
              <li><strong>名称：</strong>执行器描述，便于识别。</li>
              <li><strong>注册方式：</strong>自动注册（推荐）或手动录入机器地址。</li>
            </ul>

            <h4>2.3 新建任务</h4>
            <p>进入"任务管理"界面，新增任务：</p>
            <ul>
              <li><strong>JobHandler：</strong>对应代码中 <code>@XxlJob</code> 注解的 value 值。</li>
              <li><strong>调度类型：</strong>Cron、固定速率等。</li>
              <li><strong>路由策略：</strong>轮询、随机、故障转移等。</li>
            </ul>
          </el-collapse-item>

          <!-- 3. 执行器开发 -->
          <el-collapse-item title="3. 执行器开发（Spring Boot）" name="3">
            <h4>3.1 引入依赖</h4>
            <pre>&lt;dependency&gt;
    &lt;groupId&gt;io.github.wuwen5.xxl-job&lt;/groupId&gt;
    &lt;artifactId&gt;xxl-job-core&lt;/artifactId&gt;
    &lt;version&gt;${project.version}&lt;/version&gt;
&lt;/dependency&gt;</pre>

            <h4>3.2 配置执行器 Bean</h4>
            <pre>@Configuration
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.admin.accessToken}")
    private String accessToken;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname(appname);
        executor.setPort(port);
        executor.setAccessToken(accessToken);
        return executor;
    }
}</pre>

            <h4>3.3 application.properties 配置</h4>
            <pre>xxl.job.admin.addresses=http://127.0.0.1:8080
xxl.job.admin.accessToken=default_token
xxl.job.executor.appname=my-executor
xxl.job.executor.port=9999
xxl.job.executor.logpath=/data/applogs/xxl-job/jobhandler
xxl.job.executor.logretentiondays=30</pre>
          </el-collapse-item>

          <!-- 4. @XxlJob 注解详解 -->
          <el-collapse-item title="4. @XxlJob 注解详解" name="4">
            <p>
              <code>@XxlJob</code> 注解用于标记 Bean 中的方法为任务处理器（JobHandler），是 Bean
              模式开发任务的核心注解。标注后由执行器自动扫描注册，无需手动实现 <code>IJobHandler</code> 接口。
            </p>

            <h4>4.1 基础用法</h4>
            <p>最简写法，仅指定 handler 名称，需在调度中心手动创建任务：</p>
            <pre>@Component
public class SampleXxlJob {

    @XxlJob("demoJobHandler")
    public void demoJobHandler() throws Exception {
        XxlJobHelper.log("XXL-JOB, Hello World.");
    }
}</pre>
            <p>调度中心新建任务时，<strong>JobHandler</strong> 字段填写 <code>demoJobHandler</code> 即可关联。</p>

            <h4>4.2 生命周期回调</h4>
            <p>通过 <code>init</code> 和 <code>destroy</code> 属性指定初始化和销毁方法：</p>
            <pre>@XxlJob(value = "demoJobHandler2", init = "init", destroy = "destroy")
public void demoJobHandler2() throws Exception {
    XxlJobHelper.log("XXL-JOB, Hello World.");
}

public void init() {
    logger.info("JobHandler init");
}

public void destroy() {
    logger.info("JobHandler destroy");
}</pre>
            <ul>
              <li><code>init</code>：JobThread 初始化时调用，适合做资源预热。</li>
              <li><code>destroy</code>：JobThread 销毁时调用，适合做资源释放。</li>
            </ul>

            <h4>4.3 自动注册任务</h4>
            <p>
              设置 <code>cron</code> 或 <code>fixedRate</code> 属性后，执行器启动时会自动将任务注册到调度中心，无需手动在管理界面创建。
            </p>
            <pre>@Component
public class AutoRegisterJob {

    // 方式一：Cron 表达式自动注册
    @XxlJob(value = "autoCronJob", cron = "0/10 * * * * ?", desc = "每10秒执行一次")
    public void autoCronJob() throws Exception {
        XxlJobHelper.log("auto registered by cron");
    }

    // 方式二：固定频率自动注册（单位：秒）
    @XxlJob(value = "autoFixedRateJob", fixedRate = 30, desc = "每30秒执行一次")
    public void autoFixedRateJob() throws Exception {
        XxlJobHelper.log("auto registered by fixedRate");
    }

    // 方式三：带初始参数的自动注册
    @XxlJob(value = "autoWithParam", cron = "0 0 * * * ?", desc = "每小时执行", param = "key1=val1")
    public void autoWithParam() throws Exception {
        XxlJobHelper.log("auto registered with param");
    }
}</pre>

            <h4>4.4 属性一览</h4>
            <el-table :data="annotationAttrs" border size="small">
              <el-table-column prop="attr" label="属性" width="120" />
              <el-table-column prop="type" label="类型" width="100" />
              <el-table-column prop="defaultVal" label="默认值" width="100" />
              <el-table-column prop="desc" label="说明" />
              <el-table-column prop="required" label="必填" width="70" />
            </el-table>
          </el-collapse-item>

          <!-- 5. 高级功能 -->
          <el-collapse-item title="5. 高级功能" name="5">
            <h4>5.1 路由策略</h4>
            <p>执行器集群部署时，调度中心提供丰富的路由策略：</p>
            <ul>
              <li><strong>第一个 / 最后一个：</strong>固定选择首/末节点。</li>
              <li><strong>轮询（ROUND）：</strong>依次轮询各节点。</li>
              <li><strong>随机（RANDOM）：</strong>随机选择节点。</li>
              <li><strong>一致性HASH：</strong>相同参数路由到同一节点。</li>
              <li><strong>故障转移（FAILOVER）：</strong>按顺序探测，选择可用节点。</li>
              <li><strong>忙碌转移（BUSYOVER）：</strong>探测节点状态，选择空闲节点。</li>
              <li><strong>分片广播：</strong>广播到所有节点，每个节点接收不同分片参数。</li>
            </ul>

            <h4>5.2 阻塞策略</h4>
            <p>当上一次调度尚未完成时，新的调度请求到来的处理策略：</p>
            <ul>
              <li><strong>串行执行（SERIAL_EXECUTION）：</strong>新任务排队等待，默认策略。</li>
              <li><strong>丢弃后续调度（DISCARD_LATER）：</strong>丢弃新到的调度请求。</li>
              <li><strong>覆盖之前调度（COVER_EARLY）：</strong>终止正在执行的任务，立即执行新调度。</li>
            </ul>

            <h4>5.3 调度类型</h4>
            <ul>
              <li><strong>CRON：</strong>标准 Cron 表达式触发。</li>
              <li><strong>固定速度：</strong>固定间隔触发。</li>
            </ul>

            <h4>5.4 任务超时控制</h4>
            <p>支持配置任务超时时间（秒），超时后调度中心主动终止任务线程。</p>
          </el-collapse-item>

          <!-- 6. 常见问题 -->
          <el-collapse-item title="6. 常见问题" name="6">
            <h4>Q: 执行器注册失败？</h4>
            <p>检查调度中心地址是否正确、accessToken 是否一致、网络是否可达。</p>

            <h4>Q: 任务一直处于"运行中"？</h4>
            <p>可能是任务代码阻塞未返回。检查是否有死循环或长时间等待，必要时设置任务超时时间。</p>

            <h4>Q: 自动注册的任务重复创建？</h4>
            <p>
              自动注册会根据 <code>executorHandler</code> 去重，同一执行器下相同 handlerName 不会重复创建。若需修改
              cron 等参数，直接修改注解重新部署即可。
            </p>

            <h4>Q: 调度日志在哪里查看？</h4>
            <p>进入"任务管理" → 点击任务右侧"日志"按钮，可查看调度日志和执行日志。</p>
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

const annotationAttrs = ref([
  { attr: 'value', type: 'String', defaultVal: '-', desc: 'JobHandler 名称，对应调度中心任务的 JobHandler 字段', required: '是' },
  { attr: 'init', type: 'String', defaultVal: '""', desc: 'JobThread 初始化时调用的方法名', required: '否' },
  { attr: 'destroy', type: 'String', defaultVal: '""', desc: 'JobThread 销毁时调用的方法名', required: '否' },
  { attr: 'cron', type: 'String', defaultVal: '""', desc: 'Cron 表达式，设置后任务自动注册到调度中心', required: '否' },
  { attr: 'fixedRate', type: 'long', defaultVal: '-1', desc: '固定频率（秒），设置后任务自动注册到调度中心', required: '否' },
  { attr: 'desc', type: 'String', defaultVal: '""', desc: '任务描述，用于自动注册时显示在调度中心', required: '否' },
  { attr: 'param', type: 'String', defaultVal: '""', desc: '任务初始参数，自动注册时写入任务配置', required: '否' },
])
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

    ul,
    ol {
      padding-left: 20px;
      color: #606266;
      line-height: 2;
    }

    code {
      background-color: #f0f2f5;
      padding: 2px 6px;
      border-radius: 3px;
      font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
      font-size: 13px;
      color: #c7254e;
    }

    pre {
      background-color: #f5f7fa;
      padding: 15px;
      border-radius: 4px;
      overflow-x: auto;
      font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
      font-size: 13px;
      line-height: 1.6;
      color: #333;
      border: 1px solid #e8e8e8;
    }

    .el-table {
      margin-top: 10px;
    }
  }
}
</style>
