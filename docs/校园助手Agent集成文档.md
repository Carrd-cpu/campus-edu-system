# 校园助手 Agent 从零集成教程（Tool Calling + RAG + Spring AI）

> 面向小白：按顺序做，**新建/覆盖文件 → 粘贴代码 → 改完 Java 必须重启后端**。  
> 风格对齐《图书管理系统 AI助手集成文档》：一步一类文件，前后端都写全。  
> 完成后：学生登录后可使用 **校园助手**——查成绩/课表/公告走 **Tool Calling**；问校规走 **RAG**；知识库未命中经 **Spring AI（DeepSeek）** 兜底。

---

## 写在前面：这个功能在干什么？

1. 用户在页面输入问题。
2. 后端 `CampusAgentService` **先判断意图**（成绩、课表、公告、校规、学习计划等）。
3. **Tool Calling**：按意图调用 `EduQueryService` / `ISemesterService` / `INoticeService` / `IKnowledgeService` 等查**真实数据**。
4. **RAG**：制度类问题检索知识库切块；命中才展示参考资料。
5. **组织回答**：成绩/课表/公告等结构化结果本地拼装；知识库未命中时用 Spring AI 调 DeepSeek。
6. 前端用 **SSE** 流式展示过程步骤与正文。

**你要准备这些代码：**

| 类型              | 作用                                           |
| --------------- | -------------------------------------------- |
| 表 + 实体 + Mapper | `agent_session` / `agent_message`            |
| DTO             | 请求、响应、会话视图、步骤轨迹                              |
| Service         | `CampusAgentService`（编排核心）                   |
| Controller      | `UserEduController` 中助手相关接口（建议整文件对齐仓库）       |
| 配置              | `pom.xml` Spring AI 依赖 + `application.yml`   |
| 前端              | 路由、`vite` 代理、`UserLayout` 导航、`Assistant.vue` |

---

## 第 0 步：前置条件

1. 教务主数据已就绪（学院/班级/学期/课程/开课/成绩/公告）。
2. 文档库可上传（`kb_document` / `kb_chunk`），校规问答才有资料。
3. 用户登录 JWT 可用（`AuthContext.requireUser`）。
4. 已注册 [DeepSeek 开放平台](https://platform.deepseek.com/)（可选；不配也能本地查成绩课表）。
5. Spring Boot **3.4+**（本仓库 3.4.5），才能稳定使用 Spring AI 1.0。
6. 库名默认 `student_grade`。

---

## 第 1 步：数据库建表

表可由 `EduSchemaRunner` 启动时自动创建。也可手动执行：

```sql
CREATE TABLE IF NOT EXISTS agent_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(128) NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_agent_session_user (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS agent_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  role VARCHAR(16) NOT NULL,
  content TEXT NOT NULL,
  citations_json TEXT NULL,
  tool_trace TEXT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_agent_message_session (session_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 第 2 步：pom 引入 Spring AI

打开 `backend/pom.xml`，确保有（版本以仓库为准）：

```xml

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.9</mybatis-plus.version>
        <springdoc.version>2.6.0</springdoc.version>
        <jjwt.version>0.12.6</jjwt.version>
        <spring-ai.version>1.0.0</spring-ai.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>



    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>





    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>



            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-openai</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
```

父工程建议：

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.4.5</version>
</parent>
```

---

## 第 3 步：配置 application.yml

文件：`backend/src/main/resources/application.yml`

DeepSeek 走 OpenAI 兼容协议，由 Spring AI 调用：

```yaml
spring:
  application:
    name: springboot-vue-scaffold
  ai:
    openai:
      # DeepSeek 兼容 OpenAI 协议，通过 Spring AI 调用
      api-key: ${DEEPSEEK_API_KEY:sk-your-deepseek-api-key}
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
          temperature: 0.3
  datasource:
    url: jdbc:mysql://localhost:3306/student_grade?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: ${DB_PASSWORD:123456}
    driver-class-name: com.mysql.cj.jdbc.Driver
  sql:
    init:
      mode: never
      encoding: UTF-8
  servlet:
    multipart:
      max-file-size: 500MB
      max-request-size: 500MB

server:
  port: 8080
  servlet:
    context-path: /api

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html

logging:
  level:
    com.scaffold: debug

app:
  jwt:
    secret: change-this-secret-key-to-a-long-random-string-in-production
    expiration-ms: 86400000
  upload:
    path: uploads
  deepseek:
    # 总开关；具体密钥/地址走 spring.ai.openai.*
    enabled: true
    model: deepseek-chat
```

PowerShell 临时设置 Key：

```powershell
$env:DEEPSEEK_API_KEY="sk-你的密钥"
cd backend
mvn spring-boot:run
```

---

## 第 4 步：实体 Entity

### 4.1 AgentSession

**文件：** `backend/src/main/java/com/scaffold/entity/AgentSession.java`

```java
package com.scaffold.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_session")
public class AgentSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 4.2 AgentMessage

**文件：** `backend/src/main/java/com/scaffold/entity/AgentMessage.java`

```java
package com.scaffold.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_message")
public class AgentMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private String citations;
    private String toolTrace;
    private LocalDateTime createdAt;
}
```

---

## 第 5 步：Mapper

### 5.1 AgentSessionMapper

**文件：** `backend/src/main/java/com/scaffold/mapper/AgentSessionMapper.java`

```java
package com.scaffold.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.entity.AgentSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentSessionMapper extends BaseMapper<AgentSession> {
}
```

### 5.2 AgentMessageMapper

**文件：** `backend/src/main/java/com/scaffold/mapper/AgentMessageMapper.java`

```java
package com.scaffold.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.entity.AgentMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentMessageMapper extends BaseMapper<AgentMessage> {
}
```

---

## 第 6 步：DTO

### 6.1 AgentChatRequest

```java
package com.scaffold.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentChatRequest {
    private Long sessionId;
    private Long studentId;
    @NotBlank(message = "请输入问题")
    private String message;
}
```

### 6.2 AgentChatResponse

```java
package com.scaffold.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AgentChatResponse {
    private Long sessionId;
    private String answer;
    private boolean usedLlm;
    private List<String> steps = new ArrayList<>();
    private List<Citation> citations = new ArrayList<>();

    @Data
    public static class Citation {
        private String title;
        private String category;
        private String snippet;
    }
}
```

### 6.3 AgentSessionView / AgentMessageView / AgentTrace

```java
package com.scaffold.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentSessionView {
    private Long id;
    private Long userId;
    private String title;
    private String studentName;
    private String studentNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
package com.scaffold.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AgentMessageView {
    private Long id;
    private String role;
    private String content;
    private boolean usedLlm;
    private List<String> steps = new ArrayList<>();
    private List<AgentTrace> traces = new ArrayList<>();
    private List<AgentChatResponse.Citation> citations = new ArrayList<>();
    private LocalDateTime createdAt;
}
```

```java
package com.scaffold.dto;

import lombok.Data;

@Data
public class AgentTrace {
    private String phase;
    private String name;
    private String output;
}
```

---

## 第 7 步：学期解析（助手会用到）


**文件：** `backend/src/main/java/com/scaffold/edu/SemesterResolver.java`

```java
package com.scaffold.edu;

import com.scaffold.entity.EduSemester;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SemesterResolver {

    private static final Pattern YEAR_SPAN = Pattern.compile("(20\\d{2})\\s*[-~/到至]\\s*(20\\d{2})");
    private static final Pattern YEAR = Pattern.compile("(20\\d{2})学年");

    private SemesterResolver() {
    }

    public static EduSemester resolve(String question, List<EduSemester> all) {
        if (question == null || all == null || all.isEmpty()) {
            return null;
        }
        for (EduSemester item : all) {
            if (item.getName() != null && question.contains(item.getName())) {
                return item;
            }
            if (containsCode(question, item.getCode())) {
                return item;
            }
        }
        if (contains(question, "本学期", "这学期", "当前学期")) {
            return all.stream()
                    .filter(item -> item.getCurrentFlag() != null && item.getCurrentFlag() == 1)
                    .findFirst()
                    .orElse(null);
        }
        Integer term = termOf(question);
        if (term == null) {
            return null;
        }
        String year = yearOf(question);
        EduSemester fallback = null;
        for (EduSemester item : all) {
            if (!termMatches(item, term)) {
                continue;
            }
            if (year != null && matchesYear(item, year)) {
                return item;
            }
            if (fallback == null) {
                fallback = item;
            }
        }
        return fallback;
    }

    private static Integer termOf(String question) {
        if (contains(question, "第一学期", "上学期", "第1学期")) {
            return 1;
        }
        if (contains(question, "第二学期", "下学期", "第2学期")) {
            return 2;
        }
        return null;
    }

    private static boolean termMatches(EduSemester item, int term) {
        String name = item.getName() == null ? "" : item.getName();
        String code = item.getCode() == null ? "" : item.getCode();
        if (term == 1) {
            return name.contains("第一") || code.endsWith("-1") || code.endsWith("01");
        }
        return name.contains("第二") || code.endsWith("-2") || code.endsWith("02");
    }

    private static String yearOf(String question) {
        Matcher span = YEAR_SPAN.matcher(question);
        if (span.find()) {
            return span.group(1) + "-" + span.group(2);
        }
        Matcher year = YEAR.matcher(question);
        if (year.find()) {
            return year.group(1);
        }
        return null;
    }

    private static boolean matchesYear(EduSemester item, String year) {
        String name = item.getName() == null ? "" : item.getName();
        String code = item.getCode() == null ? "" : item.getCode();
        if (name.contains(year) || code.contains(year)) {
            return true;
        }
        String start = year.length() >= 4 ? year.substring(0, 4) : year;
        return name.contains(start) || code.startsWith(start);
    }

    private static boolean containsCode(String question, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        int idx = question.indexOf(code);
        while (idx >= 0) {
            int end = idx + code.length();
            char next = end < question.length() ? question.charAt(end) : ' ';
            if (!Character.isDigit(next) && next != '-') {
                return true;
            }
            idx = question.indexOf(code, idx + 1);
        }
        return false;
    }

    private static boolean contains(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
```

---


## 第 8 步：CampusAgentService（编排核心，完整）

**文件：** `backend/src/main/java/com/scaffold/service/CampusAgentService.java`

看懂这四点即可：

| 能力           | 做法                                                                        |
| ------------ | ------------------------------------------------------------------------- |
| Tool Calling | `needsScores` / `needsTimetable` / `needsNotices` 等触发后调 `EduQueryService` |
| RAG          | `needsKnowledge` 时 `knowledgeService.search`                              |
| 本地回答         | 成绩/课表/公告/`structuredOnly` 不走大模型                                           |
| Spring AI    | 知识库未命中且 `llmReady()` 时用 `ChatClient` 流式调用 DeepSeek                        |

```java
package com.scaffold.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.BusinessException;
import com.scaffold.dto.AgentChatResponse;
import com.scaffold.dto.AgentMessageView;
import com.scaffold.dto.AgentSessionView;
import com.scaffold.dto.AgentTrace;
import com.scaffold.dto.OfferingView;
import com.scaffold.dto.ScoreRow;
import com.scaffold.dto.StudentOverview;
import com.scaffold.edu.SemesterResolver;
import com.scaffold.entity.AgentMessage;
import com.scaffold.entity.AgentSession;
import com.scaffold.entity.EduNotice;
import com.scaffold.entity.EduSemester;
import com.scaffold.mapper.AgentMessageMapper;
import com.scaffold.mapper.AgentSessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class CampusAgentService {

    private static final Logger log = LoggerFactory.getLogger(CampusAgentService.class);
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<AgentChatResponse.Citation>> CITATION_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<AgentTrace>> TRACE_LIST = new TypeReference<>() {
    };

    @Autowired
    private AgentSessionMapper sessionMapper;
    @Autowired
    private AgentMessageMapper messageMapper;
    @Autowired
    private IKnowledgeService knowledgeService;
    @Autowired
    private EduQueryService eduQueryService;
    @Autowired
    private ISemesterService semesterService;
    @Autowired
    private INoticeService noticeService;
    @Autowired
    private IStudyPlanService studyPlanService;
    @Autowired
    private ObjectMapper objectMapper;
    /** Spring AI ChatClient；未启用模型时可能为空 */
    @Autowired
    private ObjectProvider<ChatClient.Builder> chatClientBuilder;

    @Value("${app.deepseek.enabled:true}")
    private boolean llmEnabled;
    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;
    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String model;

    public AgentChatResponse chat(Long studentId, Long sessionId, String message, boolean persist) {
        return chat(studentId, sessionId, message, persist, event -> {
        });
    }

    public AgentChatResponse chat(Long studentId, Long sessionId, String message, boolean persist,
                                  Consumer<Map<String, Object>> emit) {
        String question = message == null ? "" : message.trim();
        if (!StringUtils.hasText(question)) {
            throw new BusinessException("请输入问题");
        }
        if (question.length() > 500) {
            throw new BusinessException("问题请控制在 500 字以内");
        }
        AgentSession session = null;
        if (persist) {
            session = openSession(studentId, sessionId, question);
            saveMessage(session.getId(), "user", question, List.of(), List.of(), List.of(), false);
        }

        List<String> steps = new ArrayList<>();
        List<AgentTrace> traces = new ArrayList<>();
        EduSemester semester = SemesterResolver.resolve(question, semesterService.list());
        Long semesterId = semester == null ? null : semester.getId();
        note(emit, traces, steps, "理解问题：" + intentLabel(question, semester));
        if (isSaveRequest(question)) {
            String reply = persistLatestPlan(studentId, session == null ? null : session.getId());
            tool(emit, traces, steps, "保存学习计划", reply);
            StringBuilder answer = new StringBuilder();
            streamPieces(reply, answer, emit);
            return finish(session, persist, answer.toString().trim(), List.of(), steps, traces, false, emit);
        }
        StringBuilder observations = new StringBuilder();
        List<ScoreRow> scores = List.of();
        boolean plan = needsPlan(question);
        if (!plan && needsScores(question)) {
            String name = semester == null ? "查询我的成绩" : "查询" + semester.getName() + "成绩";
            scores = eduQueryService.scoresOf(studentId, semesterId);
            String text = formatScores(scores);
            tool(emit, traces, steps, name, text);
            observations.append("【我的成绩】\n").append(text).append("\n");
        }
        // 查成绩时一并带上绩点概况；「平均分/统计」不会再误触发（见 needsGpa）
        if (!plan && (needsGpa(question) || needsScores(question))) {
            StudentOverview overview = eduQueryService.overview(studentId);
            StringBuilder line = new StringBuilder();
            line.append("姓名 ").append(overview.getStudentName())
                    .append("，学号 ").append(empty(overview.getStudentNo()))
                    .append("，班级 ").append(empty(overview.getClazzName()))
                    .append("，累计绩点 ").append(overview.getGpa());
            if (semester != null) {
                line.append("，查询学期 ").append(semester.getName());
                if (!scores.isEmpty()) {
                    line.append("，该学期课程 ").append(scores.size()).append(" 门");
                }
            } else {
                line.append("，当前学期 ").append(empty(overview.getSemesterName()))
                        .append("，学期加权均分 ").append(overview.getSemesterAvg());
            }
            line.append("。");
            tool(emit, traces, steps, "查询绩点与学籍概况", line.toString());
            observations.append("【绩点】").append(line).append("\n");
        }
        if (!plan && needsTimetable(question)) {
            Integer day = askedWeekday(question);
            String text = formatTimetable(eduQueryService.timetable(studentId), day, question);
            tool(emit, traces, steps, timetableToolName(question, day), text);
            observations.append("【课表】\n").append(text).append("\n");
        }
        List<OfferingView> planTable = List.of();
        List<ScoreRow> planScores = List.of();
        if (plan) {
            planTable = eduQueryService.timetable(studentId);
            tool(emit, traces, steps, "查询本学期课表", formatTimetable(planTable));
            planScores = eduQueryService.scoresOf(studentId, null);
            tool(emit, traces, steps, "查询我的成绩", formatScores(planScores));
        }
        if (needsNotices(question)) {
            String text = formatNotices(noticeService.publishedList());
            tool(emit, traces, steps, "查询校园公告", text);
            observations.append("【公告】\n").append(text).append("\n");
        }
        if (needsStats(question)) {
            String text = eduQueryService.courseStatsText(question);
            tool(emit, traces, steps, "查询课程统计", text);
            observations.append("【课程统计】\n").append(text).append("\n");
        }
        List<AgentChatResponse.Citation> citations = List.of();
        String knowledge = "";
        if (needsKnowledge(question)) {
            String query = knowledgeQuery(question, plan, planTable);
            citations = knowledgeService.search(query, 3);
            if (plan) {
                // 学习计划只保留真正相关的讲义/大纲，校规章噪声不展示
                String focusCourse = namedCourseInQuestion(planTable, question);
                citations = filterPlanCitations(citations, focusCourse);
            }
            knowledge = formatCitations(citations);
            String hit = citations.isEmpty()
                    ? "知识库未命中可以直接引用的段落"
                    : citations.stream().map(AgentChatResponse.Citation::getTitle).distinct().reduce((a, b) -> a + "、" + b).orElse("");
            tool(emit, traces, steps, "检索知识库", citations.isEmpty()
                    ? (plan ? "没有这些课程的讲义，按通用安排生成" : "未命中")
                    : knowledge);
            if (!citations.isEmpty()) {
                steps.add("命中文档：" + hit);
            }
        }
        String builtPlan = "";
        if (plan) {
            builtPlan = buildStudyPlan(planTable, planScores, citations, question);
            tool(emit, traces, steps, "生成学习计划", builtPlan);
            observations.append(builtPlan).append("\n");
        }
        boolean usedLlm = false;
        StringBuilder answer = new StringBuilder();
        // 课表 / 成绩 / 绩点 / 公告 / 统计已是结构化结果，本地直接展示，不走 DeepSeek
        boolean structuredOnly = StringUtils.hasText(observations.toString())
                && !needsKnowledge(question)
                && builtPlan.isBlank();
        if (!builtPlan.isBlank()) {
            note(emit, traces, steps, "整理学习计划");
            streamPieces(withSaveHint(builtPlan, question), answer, emit);
        } else if (structuredOnly || isGreeting(question)) {
            note(emit, traces, steps, "整理查询结果");
            streamPieces(withPlanFollowUp(
                    localAnswer(question, observations.toString(), citations), question), answer, emit);
        } else if (!citations.isEmpty()) {
            // 知识库已命中时优先本地引用，避免 DeepSeek 不可用时整段落空
            note(emit, traces, steps, "整理知识库回答");
            streamPieces(localAnswer(question, observations.toString(), citations), answer, emit);
        } else if (llmReady()) {
            // 知识库未命中：交给 DeepSeek 做一般性问答（可结合工具结果）
            try {
                note(emit, traces, steps, "知识库未命中，改用 DeepSeek 问答");
                streamModel(session == null ? null : session.getId(), question, observations.toString(), knowledge, piece -> {
                    answer.append(piece);
                    emit.accept(Map.of("type", "delta", "text", piece));
                });
                usedLlm = true;
            } catch (Exception ex) {
                log.warn("DeepSeek 调用失败，改用本地编排: {}", ex.getMessage());
                if (!answer.isEmpty()) {
                    answer.setLength(0);
                    emit.accept(Map.of("type", "reset"));
                }
                note(emit, traces, steps, "组织回答：模型不可用，已改用本地编排");
                String fallback = localAnswer(question, observations.toString(), citations);
                if (!StringUtils.hasText(observations.toString()) && citations.isEmpty()) {
                    fallback = "知识库没有直接对上的段落，且 DeepSeek 暂时连不上。请稍后再试，或换个说法；也可让管理员在「知识库」补充文档。";
                }
                streamPieces(fallback, answer, emit);
            }
        } else {
            note(emit, traces, steps, "组织回答：本地编排");
            streamPieces(localAnswer(question, observations.toString(), citations), answer, emit);
        }
        return finish(session, persist, answer.toString().trim(), citations, steps, traces, usedLlm, emit);
    }

    private AgentChatResponse finish(AgentSession session, boolean persist, String finalAnswer,
                                     List<AgentChatResponse.Citation> citations, List<String> steps,
                                     List<AgentTrace> traces, boolean usedLlm, Consumer<Map<String, Object>> emit) {
        if (persist && session != null) {
            saveMessage(session.getId(), "assistant", finalAnswer, citations, steps, traces, usedLlm);
            session.setUpdatedAt(java.time.LocalDateTime.now());
            sessionMapper.updateById(session);
        }
        Map<String, Object> done = new LinkedHashMap<>();
        done.put("type", "done");
        done.put("sessionId", session == null ? null : session.getId());
        done.put("usedLlm", usedLlm);
        done.put("citations", citations);
        emit.accept(done);
        AgentChatResponse response = new AgentChatResponse();
        response.setSessionId(session == null ? null : session.getId());
        response.setAnswer(finalAnswer);
        response.setUsedLlm(usedLlm);
        response.setSteps(steps);
        response.setCitations(citations);
        return response;
    }

    private void tool(Consumer<Map<String, Object>> emit, List<AgentTrace> traces, List<String> steps,
                      String name, String output) {
        Map<String, Object> start = new LinkedHashMap<>();
        start.put("type", "trace");
        start.put("phase", "start");
        start.put("name", name);
        emit.accept(start);
        traces.add(trace("start", name, null));
        steps.add("开始：" + name);
        pause();
        String shown = brief(output);
        Map<String, Object> done = new LinkedHashMap<>();
        done.put("type", "trace");
        done.put("phase", "done");
        done.put("name", name);
        done.put("output", shown);
        emit.accept(done);
        traces.add(trace("done", name, shown));
        steps.add("完成：" + name);
        pause();
    }

    private void note(Consumer<Map<String, Object>> emit, List<AgentTrace> traces, List<String> steps, String name) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "trace");
        event.put("phase", "start");
        event.put("name", name);
        emit.accept(event);
        traces.add(trace("start", name, null));
        steps.add(name);
        pause();
    }

    private AgentTrace trace(String phase, String name, String output) {
        AgentTrace row = new AgentTrace();
        row.setPhase(phase);
        row.setName(name);
        row.setOutput(output);
        return row;
    }

    private String brief(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String one = text.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        return one.length() > 420 ? one.substring(0, 420) + "…" : one;
    }

    private void pause() {
        try {
            // 步骤之间稍停一下，方便看清「开始 / 完成」过程
            Thread.sleep(480);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void streamPieces(String text, StringBuilder answer, Consumer<Map<String, Object>> emit) {
        String value = text == null ? "" : text;
        int size = 8;
        for (int i = 0; i < value.length(); i += size) {
            String piece = value.substring(i, Math.min(value.length(), i + size));
            answer.append(piece);
            emit.accept(Map.of("type", "delta", "text", piece));
            pauseBrief();
        }
    }

    private void pauseBrief() {
        try {
            // 正文按字块打出，避免整段瞬间刷完
            Thread.sleep(38);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public List<AgentSessionView> sessions(Long studentId) {
        List<AgentSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<AgentSession>()
                .eq(studentId != null, AgentSession::getUserId, studentId)
                .orderByDesc(AgentSession::getUpdatedAt)
                .orderByDesc(AgentSession::getId));
        List<AgentSessionView> views = new ArrayList<>();
        for (AgentSession session : sessions) {
            AgentSessionView view = new AgentSessionView();
            view.setId(session.getId());
            view.setUserId(session.getUserId());
            view.setTitle(session.getTitle());
            view.setCreatedAt(session.getCreatedAt());
            view.setUpdatedAt(session.getUpdatedAt());
            if (studentId == null) {
                StudentOverview overview = safeOverview(session.getUserId());
                if (overview != null) {
                    view.setStudentName(overview.getStudentName());
                    view.setStudentNo(overview.getStudentNo());
                }
            }
            views.add(view);
        }
        return views;
    }

    public List<AgentMessageView> messages(Long sessionId, Long studentId) {
        AgentSession session = sessionMapper.selectById(sessionId);
        if (session == null || (studentId != null && !session.getUserId().equals(studentId))) {
            throw new BusinessException(404, "会话不存在");
        }
        List<AgentMessage> rows = messageMapper.selectList(new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getSessionId, sessionId)
                .orderByAsc(AgentMessage::getId));
        List<AgentMessageView> views = new ArrayList<>();
        for (AgentMessage row : rows) {
            views.add(toView(row));
        }
        return views;
    }

    public void deleteSession(Long sessionId, Long studentId) {
        AgentSession session = sessionMapper.selectById(sessionId);
        if (session == null || (studentId != null && !session.getUserId().equals(studentId))) {
            throw new BusinessException(404, "会话不存在");
        }
        messageMapper.delete(new LambdaQueryWrapper<AgentMessage>().eq(AgentMessage::getSessionId, sessionId));
        sessionMapper.deleteById(sessionId);
    }

    /** 学生端开启新对话（旧会话仍保留，方便后台查看） */
    public AgentSessionView createSession(Long studentId) {
        AgentSession session = new AgentSession();
        session.setUserId(studentId);
        session.setTitle("新对话");
        sessionMapper.insert(session);
        AgentSessionView view = new AgentSessionView();
        view.setId(session.getId());
        view.setUserId(session.getUserId());
        view.setTitle(session.getTitle());
        view.setCreatedAt(session.getCreatedAt());
        view.setUpdatedAt(session.getUpdatedAt());
        return view;
    }

    private AgentSession openSession(Long studentId, Long sessionId, String question) {
        if (sessionId != null) {
            AgentSession existing = sessionMapper.selectById(sessionId);
            if (existing == null || !existing.getUserId().equals(studentId)) {
                throw new BusinessException(404, "会话不存在");
            }
            return existing;
        }
        // 学生端只保留一个对话：无 sessionId 时复用最近会话
        AgentSession latest = sessionMapper.selectOne(new LambdaQueryWrapper<AgentSession>()
                .eq(AgentSession::getUserId, studentId)
                .orderByDesc(AgentSession::getUpdatedAt)
                .orderByDesc(AgentSession::getId)
                .last("LIMIT 1"));
        if (latest != null) {
            return latest;
        }
        AgentSession session = new AgentSession();
        session.setUserId(studentId);
        session.setTitle(question.length() > 18 ? question.substring(0, 18) + "…" : question);
        sessionMapper.insert(session);
        return session;
    }

    private void saveMessage(Long sessionId, String role, String content, List<AgentChatResponse.Citation> citations,
                             List<String> steps, List<AgentTrace> traces, boolean usedLlm) {
        AgentMessage row = new AgentMessage();
        row.setSessionId(sessionId);
        row.setRole(role);
        row.setContent(content);
        try {
            row.setCitations(objectMapper.writeValueAsString(citations));
            Map<String, Object> trace = new LinkedHashMap<>();
            trace.put("steps", steps);
            trace.put("traces", traces);
            trace.put("usedLlm", usedLlm);
            row.setToolTrace(objectMapper.writeValueAsString(trace));
        } catch (Exception ex) {
            row.setCitations("[]");
            row.setToolTrace("{\"steps\":[],\"traces\":[],\"usedLlm\":false}");
        }
        messageMapper.insert(row);
    }

    private AgentMessageView toView(AgentMessage row) {
        AgentMessageView view = new AgentMessageView();
        view.setId(row.getId());
        view.setRole(row.getRole());
        view.setContent(row.getContent());
        view.setCreatedAt(row.getCreatedAt());
        try {
            if (StringUtils.hasText(row.getCitations())) {
                view.setCitations(objectMapper.readValue(row.getCitations(), CITATION_LIST));
            }
            if (StringUtils.hasText(row.getToolTrace())) {
                JsonNode node = objectMapper.readTree(row.getToolTrace());
                if (node.has("steps")) {
                    view.setSteps(objectMapper.convertValue(node.get("steps"), STRING_LIST));
                }
                if (node.has("traces")) {
                    view.setTraces(objectMapper.convertValue(node.get("traces"), TRACE_LIST));
                }
                view.setUsedLlm(node.path("usedLlm").asBoolean(false));
            }
        } catch (Exception ignored) {
            view.setCitations(List.of());
            view.setSteps(List.of());
        }
        return view;
    }

    private boolean llmReady() {
        return llmEnabled && StringUtils.hasText(apiKey) && chatClientBuilder.getIfAvailable() != null;
    }

    /** 取当前会话最近几轮，供模型理解跟进问题（不含刚写入的本轮用户消息） */
    private List<Map<String, String>> recentChatTurns(Long sessionId, String currentQuestion) {
        if (sessionId == null) {
            return List.of();
        }
        List<AgentMessage> rows = messageMapper.selectList(new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getSessionId, sessionId)
                .orderByDesc(AgentMessage::getId)
                .last("LIMIT 9"));
        if (rows.isEmpty()) {
            return List.of();
        }
        Collections.reverse(rows);
        // 去掉刚保存的本轮用户消息
        if (!rows.isEmpty()) {
            AgentMessage last = rows.get(rows.size() - 1);
            if ("user".equals(last.getRole()) && currentQuestion.equals(last.getContent())) {
                rows = rows.subList(0, rows.size() - 1);
            }
        }
        List<Map<String, String>> turns = new ArrayList<>();
        for (AgentMessage row : rows) {
            if (!"user".equals(row.getRole()) && !"assistant".equals(row.getRole())) {
                continue;
            }
            String content = row.getContent() == null ? "" : row.getContent().trim();
            if (!StringUtils.hasText(content)) {
                continue;
            }
            if (content.length() > 600) {
                content = content.substring(0, 600) + "…";
            }
            turns.add(Map.of("role", row.getRole(), "content", content));
        }
        return turns;
    }

    private void streamModel(Long sessionId, String question, String observations, String knowledge,
                             Consumer<String> onDelta) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            StringBuilder buffered = new StringBuilder();
            try {
                // 先缓冲整段，成功后再向外推送，避免重试时重复/半截输出
                streamModelOnce(sessionId, question, observations, knowledge, buffered::append);
                String text = buffered.toString();
                int size = 12;
                for (int i = 0; i < text.length(); i += size) {
                    onDelta.accept(text.substring(i, Math.min(text.length(), i + size)));
                }
                return;
            } catch (Exception ex) {
                last = ex;
                log.warn("DeepSeek 第 {} 次调用失败: {}", attempt, ex.getMessage());
                try {
                    Thread.sleep(350L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
        throw last != null ? last : new IllegalStateException("DeepSeek 调用失败");
    }

    private void streamModelOnce(Long sessionId, String question, String observations, String knowledge,
                                 Consumer<String> onDelta) {
        ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
        if (builder == null) {
            throw new IllegalStateException("Spring AI ChatClient 未就绪");
        }
        boolean kbMiss = !StringUtils.hasText(knowledge);
        String system = """
                你是校园教务系统里的校园助手。优先根据用户问题、工具查询结果和知识库摘录回答。
                不要编造该生个人成绩、课表、绩点等数字；工具结果里有的个人数据必须以工具为准。
                不要回答班级排名、专业排名或任何名次，系统不提供排名数据。
                用简体中文，先给结论，再补充依据。引用制度时写明文标题。
                若有历史对话，可结合上下文理解跟进问题，但仍以本次工具结果为准。
                """;
        if (kbMiss) {
            system += """
                    当前知识库没有命中摘录。请仍尽量回答用户问题：可结合工具结果，或基于常见高校教务常识给出一般性说明；
                    并明确标注「非本校知识库原文，仅供参考，以学校正式文件为准」。不要只回复「知识库没有」。
                    """;
        } else {
            system += "有知识库摘录时，制度类回答以摘录为准，不要编造校规条文。\n";
        }
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(system));
        for (Map<String, String> turn : recentChatTurns(sessionId, question)) {
            if ("assistant".equals(turn.get("role"))) {
                messages.add(new AssistantMessage(turn.get("content")));
            } else {
                messages.add(new UserMessage(turn.get("content")));
            }
        }
        messages.add(new UserMessage("问题：" + question
                + "\n\n工具结果：\n" + (observations.isBlank() ? "无" : observations)
                + "\n\n知识库摘录：\n" + (knowledge.isBlank() ? "无（未命中，请按系统说明做一般性问答）" : knowledge)));

        StringBuilder collected = new StringBuilder();
        ChatClient client = builder.build();
        client.prompt()
                .messages(messages)
                .options(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(kbMiss ? 0.5 : 0.3)
                        .build())
                .stream()
                .content()
                .doOnNext(piece -> {
                    if (StringUtils.hasText(piece)) {
                        collected.append(piece);
                        onDelta.accept(piece);
                    }
                })
                .blockLast(Duration.ofSeconds(90));
        if (!StringUtils.hasText(collected.toString())) {
            throw new IllegalStateException("模型返回空内容");
        }
    }

    private String localAnswer(String question, String observations, List<AgentChatResponse.Citation> citations) {
        StringBuilder sb = new StringBuilder();
        if (isGreeting(question)) {
            sb.append("我是校园助手：会先弄清你的问题，再去查成绩、课表、公告或校规资料，用系统里的真实数据回答，不会凭空编造个人成绩或课表。也可以按课表帮你制定学习计划；生成后说「保存」，会同步到学习计划页。\n");
        }
        if (StringUtils.hasText(observations)) {
            sb.append(observations.trim()).append("\n\n");
        }
        if (!citations.isEmpty()) {
            sb.append("和问题最接近的校内资料：\n");
            for (AgentChatResponse.Citation citation : citations) {
                sb.append("《").append(citation.getTitle()).append("》：").append(citation.getSnippet()).append("\n");
            }
        } else if (!StringUtils.hasText(observations) && !isGreeting(question)) {
            sb.append("知识库里没有直接对上的段落。可以换个说法，或让管理员在「知识库」里补充文档。");
        }
        return sb.toString().trim();
    }

    private String formatScores(List<ScoreRow> scores) {
        if (scores.isEmpty()) {
            return "还没有录入成绩。";
        }
        StringBuilder sb = new StringBuilder();
        for (ScoreRow row : scores) {
            sb.append(row.getSemesterName()).append(" ")
                    .append(row.getCourseName())
                    .append(" 学分").append(row.getCredit())
                    .append(" 平时").append(row.getUsualScore())
                    .append(" 期末").append(row.getExamScore())
                    .append(" 总评").append(row.getTotalScore())
                    .append(" 绩点").append(row.getGradePoint())
                    .append("\n");
        }
        return sb.toString().trim();
    }

    private String formatTimetable(List<OfferingView> rows) {
        return formatTimetable(rows, null, "");
    }

    private String formatTimetable(List<OfferingView> rows, Integer day, String question) {
        if (rows == null || rows.isEmpty()) {
            return "当前学期还没有排课，或学生未分配班级。";
        }
        List<OfferingView> list = new ArrayList<>(rows);
        if (day != null) {
            list.removeIf(row -> row.getWeekday() == null || !day.equals(row.getWeekday()));
        }
        list.sort(Comparator
                .comparing((OfferingView row) -> row.getWeekday() == null ? 8 : row.getWeekday())
                .thenComparing(row -> row.getSectionStart() == null ? 0 : row.getSectionStart()));
        if (list.isEmpty()) {
            return dayLabel(question, day) + "没有课。";
        }
        StringBuilder sb = new StringBuilder();
        if (day != null) {
            sb.append(dayLabel(question, day)).append("有 ").append(list.size()).append(" 节课。\n");
        }
        for (OfferingView row : list) {
            sb.append(weekName(row.getWeekday())).append(" 第")
                    .append(row.getSectionStart()).append("-").append(row.getSectionEnd()).append("节 ")
                    .append(row.getCourseName()).append(" / ").append(empty(row.getTeacherName()))
                    .append(" / ").append(empty(row.getClassroom())).append("\n");
        }
        return sb.toString().trim();
    }

    private Integer askedWeekday(String text) {
        LocalDate today = LocalDate.now();
        if (contains(text, "今天", "今日")) {
            return today.getDayOfWeek().getValue();
        }
        if (text.contains("明天")) {
            return today.plusDays(1).getDayOfWeek().getValue();
        }
        if (text.contains("昨天")) {
            return today.minusDays(1).getDayOfWeek().getValue();
        }
        String[] names = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        String[] full = {"", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
        for (int i = 1; i <= 7; i++) {
            if (text.contains(names[i]) || text.contains(full[i]) || (i == 7 && text.contains("星期天"))) {
                return i;
            }
        }
        return null;
    }

    private String timetableToolName(String question, Integer day) {
        if (day == null) {
            return "查询本学期课表";
        }
        if (contains(question, "今天", "今日")) {
            return "查询今天的课表";
        }
        if (question.contains("明天")) {
            return "查询明天的课表";
        }
        if (question.contains("昨天")) {
            return "查询昨天的课表";
        }
        return "查询" + weekName(day) + "的课表";
    }

    private String dayLabel(String question, Integer day) {
        String week = weekName(day);
        if (contains(question, "今天", "今日")) {
            return "今天（" + week + "）";
        }
        if (question.contains("明天")) {
            return "明天（" + week + "）";
        }
        if (question.contains("昨天")) {
            return "昨天（" + week + "）";
        }
        return week;
    }

    private String knowledgeQuery(String question, boolean plan, List<OfferingView> planTable) {
        if (!plan) {
            return question;
        }
        String course = namedCourseInQuestion(planTable, question);
        if (StringUtils.hasText(course)) {
            return course + " 讲义 大纲 课件 学习资料";
        }
        // 未点名课程时不要把整张课表塞进检索，避免误命中校规
        return "讲义 大纲 课件 " + question;
    }

    /** 从提问里匹配课表课程名（优先更长匹配，如「数据结构」） */
    private String namedCourseInQuestion(List<OfferingView> planTable, String question) {
        OfferingView row = matchNamedCourse(planTable, question);
        return row == null ? null : row.getCourseName();
    }

    private OfferingView matchNamedCourse(List<OfferingView> rows, String question) {
        if (rows == null || rows.isEmpty() || !StringUtils.hasText(question)) {
            return null;
        }
        OfferingView best = null;
        int bestLen = 0;
        for (OfferingView row : rows) {
            String name = row.getCourseName();
            if (!StringUtils.hasText(name)) {
                continue;
            }
            if (question.contains(name) && name.length() > bestLen) {
                best = row;
                bestLen = name.length();
            }
        }
        return best;
    }

    /** 学习计划场景：丢掉与课程讲义无关的制度文档 */
    private List<AgentChatResponse.Citation> filterPlanCitations(List<AgentChatResponse.Citation> citations,
                                                                 String course) {
        if (citations == null || citations.isEmpty()) {
            return List.of();
        }
        List<AgentChatResponse.Citation> kept = new ArrayList<>();
        for (AgentChatResponse.Citation citation : citations) {
            String title = citation.getTitle() == null ? "" : citation.getTitle();
            String snippet = citation.getSnippet() == null ? "" : citation.getSnippet();
            String blob = title + " " + snippet;
            if (contains(blob, "借用", "调课", "补考", "重修", "奖学金", "考试纪律", "选课", "退课",
                    "绩点计算", "违纪", "缓考")) {
                continue;
            }
            if (StringUtils.hasText(course) && !blob.contains(course)) {
                continue;
            }
            boolean material = contains(blob, "讲义", "大纲", "课件", "教材", "习题", "复习要点", "知识点");
            boolean titled = StringUtils.hasText(course) && title.contains(course);
            if (material || titled) {
                kept.add(citation);
            }
        }
        return kept;
    }

    private boolean needsKnowledge(String text) {
        if (isGreeting(text)) {
            return false;
        }
        if (needsPlan(text) || contains(text, "规定", "办法", "纪律", "奖学金", "补考", "重修", "违纪", "缓考",
                "选课", "退课", "校规", "制度", "怎么算", "如何计算", "怎么计算", "借用", "调课", "教室借用", "借教室")) {
            return true;
        }
        // 「挂科/不及格怎么办」既要查成绩，也要给补考重修规定
        if (contains(text, "挂科", "不及格") && contains(text, "怎么办", "怎么处理", "如何处理", "咋办")) {
            return true;
        }
        return !(needsTimetable(text) || needsScores(text) || needsGpa(text) || needsNotices(text) || needsStats(text));
    }

    private String formatNotices(List<EduNotice> notices) {
        if (notices.isEmpty()) {
            return "暂无公告。";
        }
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (EduNotice notice : notices) {
            if (count++ >= 4) {
                break;
            }
            String content = notice.getContent() == null ? "" : notice.getContent()
                    .replaceAll("(?is)<[^>]+>", " ")
                    .replace("&nbsp;", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            if (content.length() > 80) {
                content = content.substring(0, 80) + "…";
            }
            sb.append(notice.getTitle()).append("：").append(content).append("\n");
        }
        return sb.toString().trim();
    }

    private String formatCitations(List<AgentChatResponse.Citation> citations) {
        StringBuilder sb = new StringBuilder();
        for (AgentChatResponse.Citation citation : citations) {
            sb.append("《").append(citation.getTitle()).append("》").append(citation.getSnippet()).append("\n");
        }
        return sb.toString().trim();
    }

    private StudentOverview safeOverview(Long userId) {
        try {
            return eduQueryService.overview(userId);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean needsScores(String text) {
        return contains(text, "成绩", "分数", "考了", "挂科", "不及格", "总评");
    }

    private boolean needsGpa(String text) {
        // 避免「平均分 / 统计」误触发绩点概况
        return contains(text, "绩点", "GPA", "gpa", "累计绩点", "加权平均");
    }

    private boolean needsTimetable(String text) {
        // 「教室借用 / 调课说明」是制度问答，不要当成查课表
        if (contains(text, "借用", "调课", "借教室", "教室借用", "申请教室")) {
            return false;
        }
        // 注意：「课程表」不含连续「课表」二字，需单独匹配
        return contains(text, "课表", "课程表", "排课", "上课", "周几", "星期", "课程安排",
                "有课", "什么课", "哪节", "在哪上课", "哪间教室", "我的课");
    }

    private boolean needsNotices(String text) {
        return contains(text, "公告", "通知", "放假", "考试安排", "期末考试");
    }

    private boolean needsStats(String text) {
        return contains(text, "及格率", "统计", "平均分", "哪门", "分布");
    }

    private boolean needsPlan(String text) {
        return contains(text, "学习计划", "复习计划", "复习安排", "学习安排", "怎么复习", "复习方向", "整周计划");
    }

    private boolean isSaveRequest(String text) {
        String value = text == null ? "" : text.trim();
        if (!value.contains("保存") || contains(value, "不要保存", "别保存", "怎么保存", "如何保存")) {
            return false;
        }
        // 「制定/生成学习计划」优先走生成，不要误当成保存
        if (contains(value, "制定", "生成", "帮我做", "安排一份", "出一份", "做一份")) {
            return false;
        }
        if (contains(value, "保存学习计划", "保存计划", "保存到学习计划", "存到学习计划", "保存一下")) {
            return true;
        }
        return value.length() <= 40;
    }

    private String persistLatestPlan(Long studentId, Long sessionId) {
        String found = findAndSavePlan(studentId, sessionId);
        if (found != null) {
            return found;
        }
        // 当前会话没有时，到该生其他会话里找最近一份带【计划安排】的回复
        List<AgentSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<AgentSession>()
                .eq(AgentSession::getUserId, studentId)
                .orderByDesc(AgentSession::getUpdatedAt)
                .orderByDesc(AgentSession::getId)
                .last("LIMIT 8"));
        for (AgentSession session : sessions) {
            if (sessionId != null && sessionId.equals(session.getId())) {
                continue;
            }
            found = findAndSavePlan(studentId, session.getId());
            if (found != null) {
                return found;
            }
        }
        if (sessions.isEmpty()) {
            return "还没有对话。先让我生成一份学习计划，再说「保存」。";
        }
        return "还没有可保存的学习计划。可以先说「帮我制定学习计划」，生成后再说「保存」。";
    }

    private String findAndSavePlan(Long studentId, Long sessionId) {
        if (sessionId == null) {
            return null;
        }
        List<AgentMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getSessionId, sessionId)
                .eq(AgentMessage::getRole, "assistant")
                .orderByDesc(AgentMessage::getId));
        for (AgentMessage message : messages) {
            String content = message.getContent();
            if (content == null || content.startsWith("已保存到学习计划") || !content.contains("【计划安排】")) {
                continue;
            }
            String body = stripSaveHint(content);
            studyPlanService.save(studentId, body, true);
            return "已保存到学习计划。打开右上角头像菜单里的「学习计划」可以查看。\n\n" + body;
        }
        return null;
    }

    private String stripSaveHint(String content) {
        int index = content.indexOf("需要的话");
        String body = index >= 0 ? content.substring(0, index) : content;
        return body.trim();
    }

    private String withSaveHint(String plan, String question) {
        // 没排出可执行计划时，不要再提示「保存 / 换复习方向」
        if (plan.contains("暂时无法")) {
            return plan;
        }
        return plan + "\n\n需要的话，我可以：\n" + saveHintLines(plan, question);
    }

    /** 查完课表 / 成绩后，引导继续制定学习计划 */
    private String withPlanFollowUp(String answer, String question) {
        if (!StringUtils.hasText(answer) || isGreeting(question) || needsPlan(question) || isSaveRequest(question)) {
            return answer;
        }
        if (!(needsScores(question) || needsTimetable(question) || needsGpa(question))) {
            return answer;
        }
        if (answer.contains("暂时无法") || answer.contains("还没有排课") || answer.contains("学生未分配班级")) {
            return answer;
        }
        return answer + "\n\n需要的话，我可以：\n"
                + "按这份课表/成绩帮你制定学习计划（说「帮我制定学习计划」即可）；\n"
                + "生成后再说「保存」，会同步到学习计划页。";
    }

    private String saveHintLines(String plan, String question) {
        boolean review = contains(question, "复习");
        boolean weekly = contains(question, "本周", "整周", "一周", "其他课", "都排");
        StringBuilder sb = new StringBuilder();
        sb.append("把它保存成你的学习计划（直接回复「保存」或「保存学习计划」）；\n");
        if (review) {
            sb.append("换回预习方向的计划（针对下一节课）；\n");
        } else {
            sb.append("换成复习方向的计划（针对今天已上的那节）；\n");
        }
        if (weekly) {
            sb.append("本周其他课程已按成绩排进上面的安排，分数越低时间越长。");
        } else {
            String names = namedOthers(plan);
            sb.append(names.isBlank()
                    ? "顺带把其他课也排进去，做一份整周计划。"
                    : "顺带把 " + names + " 也排进去，做一份整周计划。");
        }
        return sb.toString();
    }

    private String namedOthers(String plan) {
        int index = plan.lastIndexOf("其他课程：");
        if (index < 0) {
            return "";
        }
        String tail = plan.substring(index + "其他课程：".length()).trim();
        int end = tail.indexOf('。');
        return end >= 0 ? tail.substring(0, end) : tail;
    }

    private String buildStudyPlan(List<OfferingView> timetable, List<ScoreRow> scores,
                                  List<AgentChatResponse.Citation> citations, String question) {
        if (timetable == null || timetable.isEmpty()) {
            return "当前学期还没有排课，暂时无法按课表制定学习计划。等开课安排好之后，再让我按课表帮你排。";
        }
        Map<String, BigDecimal> worst = new HashMap<>();
        if (scores != null) {
            for (ScoreRow row : scores) {
                if (!StringUtils.hasText(row.getCourseName()) || row.getTotalScore() == null) {
                    continue;
                }
                worst.merge(row.getCourseName(), row.getTotalScore(), BigDecimal::min);
            }
        }
        List<OfferingView> rows = new ArrayList<>();
        for (OfferingView row : timetable) {
            int weekday = row.getWeekday() == null ? 0 : row.getWeekday();
            if (weekday >= 1 && weekday <= 7 && StringUtils.hasText(row.getCourseName())) {
                rows.add(row);
            }
        }
        rows.sort(Comparator
                .comparing((OfferingView row) -> row.getWeekday())
                .thenComparing(row -> row.getSectionStart() == null ? 0 : row.getSectionStart()));
        if (rows.isEmpty()) {
            return "课表里还没有可用的上课时间，暂时无法制定学习计划。等排课节次补全后再试一次。";
        }
        boolean review = contains(question, "复习");
        boolean weekly = contains(question, "本周", "整周", "一周", "其他课", "都排");
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        OfferingView focus = pickFocus(rows, monday, today, review, question);
        BigDecimal score = worst.get(focus.getCourseName());
        StringBuilder sb = new StringBuilder();
        sb.append(focus.getCourseName()).append(review ? "复习学习计划" : "预习学习计划")
                .append("（").append(whenText(focus, monday, today, review)).append("）\n\n");
        // 无相关讲义时只写资料说明，不把校规塞进「参考资料」
        sb.append("【资料说明】").append(sourceNote(focus.getCourseName(), citations, score)).append("\n\n");
        sb.append("【课程时间】\n").append(slotText(focus, monday, today, review)).append("\n\n");
        sb.append("【计划安排】\n").append(arrangement(focus.getCourseName(), score, review));
        if (weekly) {
            sb.append("\n\n【本周其他课程】\n").append(weeklyLines(rows, focus, worst, monday, today));
        }
        sb.append("\n\n【提醒】\n");
        sb.append("预习不要试图一次看懂所有推导，重点是「知道要学什么 + 找出疑问」。\n");
        sb.append(focus.getCourseName()).append("靠连续性，每天 30-40 分钟比周末突击 3 小时更有效。\n");
        sb.append("做题先手算再对答案，错题当天记下来。");
        if (!weekly) {
            String others = otherNames(rows, focus);
            if (StringUtils.hasText(others)) {
                sb.append("\n\n其他课程：").append(others).append("。说「排成整周计划」可以把它们也排进去。");
            }
        }
        return sb.toString().trim();
    }

    private OfferingView pickFocus(List<OfferingView> rows, LocalDate monday, LocalDate today,
                                   boolean review, String question) {
        OfferingView named = matchNamedCourse(rows, question);
        if (named != null) {
            return named;
        }
        OfferingView past = null;
        OfferingView upcoming = null;
        for (OfferingView row : rows) {
            LocalDate date = monday.plusDays(row.getWeekday() - 1L);
            if (!date.isAfter(today)) {
                past = row;
            } else if (upcoming == null) {
                upcoming = row;
            }
        }
        if (review) {
            return past != null ? past : rows.get(rows.size() - 1);
        }
        return upcoming != null ? upcoming : rows.get(0);
    }

    private String whenText(OfferingView row, LocalDate monday, LocalDate today, boolean review) {
        LocalDate date = classDate(row, monday, today, review);
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " " + weekName(row.getWeekday())
                + " " + sectionText(row) + "，" + roomText(row);
    }

    private String slotText(OfferingView row, LocalDate monday, LocalDate today, boolean review) {
        LocalDate date = classDate(row, monday, today, review);
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "（" + weekName(row.getWeekday()) + "）"
                + sectionText(row) + " " + row.getCourseName() + " " + roomText(row);
    }

    private LocalDate classDate(OfferingView row, LocalDate monday, LocalDate today, boolean review) {
        LocalDate date = monday.plusDays(row.getWeekday() - 1L);
        if (!review && date.isBefore(today)) {
            return date.plusDays(7);
        }
        return date;
    }

    private String sourceNote(String course, List<AgentChatResponse.Citation> citations, BigDecimal score) {
        String base;
        AgentChatResponse.Citation matched = matchCourseCitation(course, citations);
        if (matched == null) {
            base = "知识库中未检索到" + course + "相关讲义或大纲，本计划为通用安排建议，具体章节以老师布置为准。";
        } else {
            base = "已参考知识库《" + matched.getTitle() + "》。具体章节以老师布置为准。";
        }
        if (score != null && score.doubleValue() < 70) {
            base += "这门课目前分数偏低，安排的时间会更长。";
        }
        return base;
    }

    private AgentChatResponse.Citation matchCourseCitation(String course,
                                                           List<AgentChatResponse.Citation> citations) {
        if (!StringUtils.hasText(course) || citations == null || citations.isEmpty()) {
            return null;
        }
        for (AgentChatResponse.Citation citation : citations) {
            String title = citation.getTitle() == null ? "" : citation.getTitle();
            String snippet = citation.getSnippet() == null ? "" : citation.getSnippet();
            if (title.contains(course) || snippet.contains(course)) {
                return citation;
            }
        }
        return null;
    }

    private String arrangement(String course, BigDecimal score, boolean review) {
        int total = planMinutes(score);
        int before = Math.max(20, total / 2);
        int practice = Math.max(10, total / 4);
        int after = Math.max(10, total - before / 2);
        if (review) {
            return "回顾（课后 " + before + " 分钟）：翻回课上笔记，确认本节衔接点，能说出本节用到的旧知识。\n"
                    + "订正（课后 " + practice + " 分钟）：做错题先手算再对答案，标出不会的步骤。\n"
                    + "巩固（当晚 " + after + " 分钟）：把错题按知识点归类，当天记下来。";
        }
        return "预习（课前 " + before + " 分钟）：通读" + course + "新章节的定义、定理、例题，先不抠细节，标出 3 处看不懂的地方。\n"
                + "试做（课前 " + practice + " 分钟）：精读定义与公式，做 2 道最基础的例题，能独立复述定义。\n"
                + "课后补漏（课后 " + after + " 分钟）：对照课上讲解回看标出的疑点，错题记下题目、错因和下次提醒。";
    }

    private String weeklyLines(List<OfferingView> rows, OfferingView focus, Map<String, BigDecimal> worst,
                               LocalDate monday, LocalDate today) {
        StringBuilder sb = new StringBuilder();
        boolean any = false;
        for (OfferingView row : rows) {
            if (sameSlot(row, focus)) {
                continue;
            }
            BigDecimal score = worst.get(row.getCourseName());
            if (any) {
                sb.append("\n");
            }
            sb.append("【计划】").append(weekName(row.getWeekday())).append(" ")
                    .append(row.getCourseName()).append(" ")
                    .append(planMinutes(score)).append("分钟 ")
                    .append(planTask(score));
            any = true;
        }
        return any ? sb.toString() : "本周没有其他课程。";
    }

    private String otherNames(List<OfferingView> rows, OfferingView focus) {
        List<String> names = new ArrayList<>();
        for (OfferingView row : rows) {
            if (sameSlot(row, focus) || names.contains(row.getCourseName())) {
                continue;
            }
            names.add(row.getCourseName());
            if (names.size() >= 4) {
                break;
            }
        }
        return String.join("、", names);
    }

    private boolean sameSlot(OfferingView left, OfferingView right) {
        return left.getWeekday().equals(right.getWeekday())
                && left.getCourseName().equals(right.getCourseName());
    }

    private String weekName(Integer weekday) {
        String[] weeks = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        int index = weekday == null ? 0 : weekday;
        return index >= 1 && index < weeks.length ? weeks[index] : "";
    }

    private String sectionText(OfferingView row) {
        int start = row.getSectionStart() == null ? 0 : row.getSectionStart();
        int end = row.getSectionEnd() == null ? start : row.getSectionEnd();
        if (start <= 0) {
            return "节次待定";
        }
        return start == end ? start + "节" : start + "-" + end + "节";
    }

    private String roomText(OfferingView row) {
        return StringUtils.hasText(row.getClassroom()) ? row.getClassroom() : "教室待定";
    }

    private int planMinutes(BigDecimal score) {
        if (score == null) {
            return 40;
        }
        double value = score.doubleValue();
        if (value >= 85) {
            return 30;
        }
        if (value >= 70) {
            return 45;
        }
        if (value >= 60) {
            return 60;
        }
        return 75;
    }

    private String planTask(BigDecimal score) {
        if (score == null) {
            return "预习下次课内容，并整理课堂笔记";
        }
        double value = score.doubleValue();
        if (value >= 85) {
            return "快速回顾课堂要点，保持手感";
        }
        if (value >= 70) {
            return "复习本节例题，再做两道练习";
        }
        if (value >= 60) {
            return "重做错过的题，补上不熟的知识点";
        }
        return "从基础例题重学，整理不懂的步骤";
    }

    private boolean isGreeting(String text) {
        String value = text.trim();
        return value.length() <= 12 && contains(value, "你好", "您好", "你是谁", "帮助", "能做什么");
    }

    private String intentLabel(String text, EduSemester semester) {
        if (isSaveRequest(text)) {
            return "保存学习计划";
        }
        List<String> labels = new ArrayList<>();
        if (needsScores(text)) {
            labels.add(semester == null ? "成绩" : semester.getName() + "成绩");
        }
        if (needsGpa(text)) {
            labels.add("绩点");
        }
        if (needsTimetable(text)) {
            labels.add("课表");
        }
        if (needsPlan(text)) {
            labels.add("学习计划");
        }
        if (needsNotices(text)) {
            labels.add("公告");
        }
        if (needsStats(text)) {
            labels.add("统计");
        }
        if (needsKnowledge(text) && (needsPlan(text) || contains(text, "规定", "办法", "纪律", "奖学金", "补考",
                "重修", "违纪", "缓考", "选课", "退课", "校规", "制度", "怎么算", "如何计算", "怎么计算",
                "借用", "调课", "教室借用", "借教室", "挂科", "不及格"))) {
            labels.add("校内规定");
        }
        if (labels.isEmpty()) {
            return "校内知识问答";
        }
        return String.join("、", labels);
    }

    private boolean contains(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String empty(String value) {
        return StringUtils.hasText(value) ? value : "未填写";
    }
}
```

---


## 第 9 步：UserEduController（完整）

**文件：** `backend/src/main/java/com/scaffold/controller/user/UserEduController.java`

助手接口：

| 方法   | 路径                                       | 说明     |
| ---- | ---------------------------------------- | ------ |
| GET  | `/user/edu/agent/sessions`               | 会话列表   |
| POST | `/user/edu/agent/sessions`               | 新对话    |
| GET  | `/user/edu/agent/sessions/{id}/messages` | 历史消息   |
| POST | `/user/edu/agent/chat`                   | 非流式    |
| POST | `/user/edu/agent/chat/stream`            | SSE 流式 |

下面给出**完整 Controller**（含成绩/课表/公告/学习计划，与仓库一致）：

```java
package com.scaffold.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.AuthContext;
import com.scaffold.common.BusinessException;
import com.scaffold.common.Result;
import com.scaffold.dto.AgentChatRequest;
import com.scaffold.dto.AgentChatResponse;
import com.scaffold.dto.AgentMessageView;
import com.scaffold.dto.AgentSessionView;
import com.scaffold.dto.OfferingView;
import com.scaffold.dto.ScoreRow;
import com.scaffold.dto.TranscriptView;
import com.scaffold.common.PageResult;
import com.scaffold.entity.EduNotice;
import com.scaffold.entity.EduStudyPlan;
import com.scaffold.service.CampusAgentService;
import com.scaffold.service.INoticeService;
import com.scaffold.service.EduQueryService;
import com.scaffold.service.IStudyPlanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user/edu")
public class UserEduController {

    @Autowired
    private EduQueryService eduQueryService;
    @Autowired
    private INoticeService noticeService;
    @Autowired
    private CampusAgentService campusAgentService;
    @Autowired
    private IStudyPlanService studyPlanService;
    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/scores")
    public Result<List<ScoreRow>> scores(HttpServletRequest request, @RequestParam(required = false) Long semesterId) {
        AuthContext.requireUser(request);
        return Result.ok(eduQueryService.scoresOf(AuthContext.getUserId(request), semesterId));
    }

    @GetMapping("/transcript")
    public Result<TranscriptView> transcript(HttpServletRequest request) {
        AuthContext.requireUser(request);
        return Result.ok(eduQueryService.transcript(AuthContext.getUserId(request)));
    }

    @GetMapping("/timetable")
    public Result<List<OfferingView>> timetable(HttpServletRequest request) {
        AuthContext.requireUser(request);
        return Result.ok(eduQueryService.timetable(AuthContext.getUserId(request)));
    }

    @GetMapping("/notices")
    public Result<List<EduNotice>> notices() {
        return Result.ok(noticeService.publishedList());
    }

    @GetMapping("/notices/{id}")
    public Result<EduNotice> notice(@PathVariable Long id) {
        return Result.ok(noticeService.publishedOne(id));
    }

    @GetMapping("/plans")
    public Result<PageResult<EduStudyPlan>> plans(HttpServletRequest request,
                                                   @RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size) {
        AuthContext.requireUser(request);
        return Result.ok(studyPlanService.page(AuthContext.getUserId(request), page, size));
    }

    @GetMapping("/plans/{id}")
    public Result<EduStudyPlan> plan(HttpServletRequest request, @PathVariable Long id) {
        AuthContext.requireUser(request);
        return Result.ok(studyPlanService.detail(AuthContext.getUserId(request), id));
    }

    @PostMapping("/plans")
    public Result<EduStudyPlan> savePlan(HttpServletRequest request, @RequestBody Map<String, String> body) {
        AuthContext.requireUser(request);
        String content = body == null ? null : body.get("content");
        return Result.ok(studyPlanService.save(AuthContext.getUserId(request), content, false));
    }

    @DeleteMapping("/plans/{id}")
    public Result<Void> removePlan(HttpServletRequest request, @PathVariable Long id) {
        AuthContext.requireUser(request);
        studyPlanService.remove(AuthContext.getUserId(request), id);
        return Result.ok();
    }

    @GetMapping("/agent/sessions")
    public Result<List<AgentSessionView>> sessions(HttpServletRequest request) {
        AuthContext.requireUser(request);
        return Result.ok(campusAgentService.sessions(AuthContext.getUserId(request)));
    }

    @PostMapping("/agent/sessions")
    public Result<AgentSessionView> createSession(HttpServletRequest request) {
        AuthContext.requireUser(request);
        return Result.ok(campusAgentService.createSession(AuthContext.getUserId(request)));
    }

    @GetMapping("/agent/sessions/{id}/messages")
    public Result<List<AgentMessageView>> messages(HttpServletRequest request, @PathVariable Long id) {
        AuthContext.requireUser(request);
        return Result.ok(campusAgentService.messages(id, AuthContext.getUserId(request)));
    }

    @PostMapping("/agent/chat")
    public Result<AgentChatResponse> chat(HttpServletRequest request, @Valid @RequestBody AgentChatRequest body) {
        AuthContext.requireUser(request);
        return Result.ok(campusAgentService.chat(AuthContext.getUserId(request), body.getSessionId(), body.getMessage(), true));
    }

    @PostMapping(value = "/agent/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(HttpServletRequest request, @Valid @RequestBody AgentChatRequest body) {
        AuthContext.requireUser(request);
        Long userId = AuthContext.getUserId(request);
        SseEmitter emitter = new SseEmitter(120_000L);
        Thread worker = new Thread(() -> {
            try {
                campusAgentService.chat(userId, body.getSessionId(), body.getMessage(), true, event -> {
                    try {
                        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(event)));
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                });
                emitter.complete();
            } catch (Exception ex) {
                try {
                    Map<String, Object> error = new LinkedHashMap<>();
                    error.put("type", "error");
                    error.put("message", ex instanceof BusinessException && ex.getMessage() != null
                            ? ex.getMessage()
                            : "助手暂时无法回答");
                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(error)));
                    emitter.complete();
                } catch (Exception ignored) {
                    emitter.completeWithError(ex);
                }
            }
        }, "agent-chat-stream");
        worker.setDaemon(true);
        worker.start();
        return emitter;
    }
}
```

---

## 第 10 步：确认 JWT（助手必须登录）

`/user/edu/agent/**` **不要**放进白名单。公开的只有公告等，例如：

```java
.excludePathPatterns(
        "/auth/login",
        "/auth/register",
        "/user/banners",
        "/user/system-config",
        "/user/edu/notices",
        "/user/edu/notices/**"
        // 注意：不要写 "/user/edu/agent/**"
);
```

---

## 第 11 步：编译并重启后端

```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

出现 `Started ScaffoldApplication` 即成功。

---

# 第二部分：前端集成（按步粘贴）

> 约定与图书教程相同：**加页面先改路由**，再写 `.vue`，用户端还要改 `UserLayout` 导航；流式接口要配好 `vite` 代理。

## 第 12 步：开发代理 vite.config.js（完整）

**文件：** `frontend/vite.config.js`

助手 SSE 路径含 `/agent/chat/stream`，代理里关闭缓冲：

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes, req) => {
            if (req.url && req.url.includes('/agent/chat/stream')) {
              proxyRes.headers['cache-control'] = 'no-cache'
              proxyRes.headers['x-accel-buffering'] = 'no'
            }
          })
        }
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

---


## 第 13 步：路由 router/index.js（完整）

**文件：** `frontend/src/router/index.js`

用户端至少包含：

```javascript
{
  path: 'assistant',
  name: 'UserAssistant',
  component: () => import('@/views/user/Assistant.vue'),
  meta: { requiresAuth: true, requiresUser: true, title: '校园助手' }
},
{
  path: 'plans',
  name: 'UserStudyPlans',
  component: () => import('@/views/user/StudyPlans.vue'),
  meta: { requiresAuth: true, requiresUser: true, title: '学习计划' }
},
```

完整路由文件：

```js
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    redirect: '/user/home'
  },
  {
    path: '/login',
    name: 'UserLogin',
    component: () => import('@/views/UserLogin.vue'),
    meta: { public: true }
  },
  {
    path: '/register',
    name: 'UserRegister',
    component: () => import('@/views/UserRegister.vue'),
    meta: { public: true }
  },
  {
    path: '/user',
    component: () => import('@/layouts/UserLayout.vue'),
    meta: { public: true },
    redirect: '/user/home',
    children: [
      {
        path: 'home',
        name: 'UserHome',
        component: () => import('@/views/user/Home.vue'),
        meta: { public: true, title: '用户首页' }
      },
      {
        path: 'scores',
        name: 'UserScores',
        component: () => import('@/views/user/Scores.vue'),
        meta: { requiresAuth: true, requiresUser: true, title: '我的成绩' }
      },
      {
        path: 'timetable',
        name: 'UserTimetable',
        component: () => import('@/views/user/Timetable.vue'),
        meta: { requiresAuth: true, requiresUser: true, title: '开课安排' }
      },
      {
        path: 'transcript',
        name: 'UserTranscript',
        component: () => import('@/views/user/Transcript.vue'),
        meta: { requiresAuth: true, requiresUser: true, title: '成绩单' }
      },
      {
        path: 'notices',
        name: 'UserNotices',
        component: () => import('@/views/user/Notices.vue'),
        meta: { public: true, title: '校园公告' }
      },
      {
        path: 'notices/:id',
        name: 'UserNoticeDetail',
        component: () => import('@/views/user/NoticeDetail.vue'),
        meta: { public: true, title: '公告详情' }
      },
      {
        path: 'plans',
        name: 'UserStudyPlans',
        component: () => import('@/views/user/StudyPlans.vue'),
        meta: { requiresAuth: true, requiresUser: true, title: '学习计划' }
      },
      {
        path: 'assistant',
        name: 'UserAssistant',
        component: () => import('@/views/user/Assistant.vue'),
        meta: { requiresAuth: true, requiresUser: true, title: '校园助手' }
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { requiresAuth: true, requiresUser: true, title: '个人中心' }
      }
    ]
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/Login.vue'),
    meta: { public: true, admin: true }
  },
  {
    path: '/admin',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/admin/home',
    meta: { admin: true, requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: 'home',
        name: 'AdminHome',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页', admin: true }
      },
      {
        path: 'stats',
        redirect: '/admin/home'
      },
      {
        path: 'colleges',
        name: 'AdminColleges',
        component: () => import('@/views/edu/Colleges.vue'),
        meta: { title: '学院管理', admin: true }
      },
      {
        path: 'majors',
        name: 'AdminMajors',
        component: () => import('@/views/edu/Majors.vue'),
        meta: { title: '专业管理', admin: true }
      },
      {
        path: 'classes',
        name: 'AdminClasses',
        component: () => import('@/views/edu/Classes.vue'),
        meta: { title: '班级管理', admin: true }
      },
      {
        path: 'semesters',
        name: 'AdminSemesters',
        component: () => import('@/views/edu/Semesters.vue'),
        meta: { title: '学期管理', admin: true }
      },
      {
        path: 'courses',
        name: 'AdminCourses',
        component: () => import('@/views/edu/Courses.vue'),
        meta: { title: '课程管理', admin: true }
      },
      {
        path: 'offerings',
        name: 'AdminOfferings',
        component: () => import('@/views/edu/Offerings.vue'),
        meta: { title: '开课安排', admin: true }
      },
      {
        path: 'scores',
        name: 'AdminScores',
        component: () => import('@/views/edu/Scores.vue'),
        meta: { title: '成绩管理', admin: true }
      },
      {
        path: 'notices',
        name: 'AdminNotices',
        component: () => import('@/views/edu/Notices.vue'),
        meta: { title: '校园公告', admin: true }
      },
      {
        path: 'knowledge',
        name: 'AdminKnowledge',
        component: () => import('@/views/edu/Knowledge.vue'),
        meta: { title: '文档库', admin: true }
      },
      {
        path: 'admins',
        name: 'AdminAdmins',
        component: () => import('@/views/Admins.vue'),
        meta: { title: '管理员管理', admin: true }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/Users.vue'),
        meta: { title: '用户管理', admin: true }
      },
      {
        path: 'menus',
        name: 'AdminMenus',
        component: () => import('@/views/Menus.vue'),
        meta: { title: '菜单管理', admin: true }
      },
      {
        path: 'banners',
        name: 'AdminBanners',
        component: () => import('@/views/system/Banners.vue'),
        meta: { title: '轮播图管理', admin: true }
      },
      {
        path: 'logs',
        name: 'AdminLogs',
        component: () => import('@/views/Logs.vue'),
        meta: { title: '系统日志', admin: true }
      },
      {
        path: 'profile',
        name: 'AdminProfile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', admin: true }
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: () => import('@/views/Settings.vue'),
        meta: { title: '系统设置', admin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (to.path.startsWith('/admin') && from.path.startsWith('/admin')) {
      const main = document.querySelector('.layout .main')
      if (main) main.scrollTop = 0
      return false
    }
    if (savedPosition) return savedPosition
    return { top: 0 }
  }
})

function isAdminAccount(user) {
  return user?.accountType === 'ADMIN' || user?.role === 'ADMIN'
}

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  const token = localStorage.getItem('token')
  const isAdminLogin = to.path === '/admin/login'
  const isUserLogin = to.path === '/login'
  const isUserRegister = to.path === '/register'
  const isAdminRoute = to.path.startsWith('/admin') && !isAdminLogin

  // 已登录访问登录/注册页时跳转
  if (to.meta.public && !to.meta.requiresAuth) {
    if (token && (isAdminLogin || isUserLogin || isUserRegister)) {
      if (!userStore.user) {
        try {
          await userStore.fetchUser()
        } catch {
          userStore.logout()
          return true
        }
      }
      if (isAdminLogin && isAdminAccount(userStore.user)) {
        return '/admin/home'
      }
      if ((isUserLogin || isUserRegister) && !isAdminAccount(userStore.user)) {
        return '/user/home'
      }
    }
    return true
  }

  // 管理后台 / 需要登录的用户页
  if (to.meta.requiresAuth || isAdminRoute) {
    if (!token) {
      const loginPath = to.meta.requiresUser ? '/login' : '/admin/login'
      return { path: loginPath, query: { redirect: to.fullPath } }
    }

    if (!userStore.user) {
      try {
        await userStore.fetchUser()
      } catch {
        userStore.logout()
        const loginPath = to.meta.requiresUser ? '/login' : '/admin/login'
        return { path: loginPath, query: { redirect: to.fullPath } }
      }
    }

    if (to.meta.requiresAdmin || isAdminRoute) {
      if (!isAdminAccount(userStore.user)) {
        userStore.logout()
        return { path: '/admin/login' }
      }
    }

    if (to.meta.requiresUser && isAdminAccount(userStore.user)) {
      return '/admin/home'
    }
  }

  return true
})

export default router
```

---


## 第 14 步：导航 UserLayout.vue（完整）

**文件：** `frontend/src/layouts/UserLayout.vue`

`navItems` 中需有校园助手；头像菜单可进助手与学习计划。

```vue
<template>
  <div class="user-shell" :class="{ chat: isChatPage }">
    <header class="user-nav" :class="{ scrolled: scrolled }">
      <div class="nav-inner">
        <router-link to="/user/home" class="brand">
          <BrandLogo size="md" class="nav-brand-logo" />
          <span class="brand-text">
            <span class="brand-name">{{ systemStore.displayName }}</span>
            <span class="brand-tagline">成绩查询 · 校园助手</span>
          </span>
        </router-link>

        <nav class="nav-links">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ 'router-link-active': isNavActive(item.path) }"
          >
            <el-icon :size="16"><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </router-link>
        </nav>

        <div class="nav-actions">
          <template v-if="isLoggedIn">
            <el-dropdown trigger="click" popper-class="user-menu" @command="onCommand">
              <button type="button" class="user-chip">
                <el-avatar :size="34" :src="avatarSrc">{{ avatarFallback }}</el-avatar>
                <span class="user-meta">
                  <span class="user-name">{{ displayName }}</span>
                  <span class="user-role">已登录</span>
                </span>
                <el-icon class="chev" :size="14"><ArrowDown /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="home">
                    <el-icon><HomeFilled /></el-icon>
                    <span>首页</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="scores">
                    <el-icon><Tickets /></el-icon>
                    <span>我的成绩</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="assistant">
                    <el-icon><ChatDotRound /></el-icon>
                    <span>校园助手</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="plans">
                    <el-icon><Notebook /></el-icon>
                    <span>学习计划</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>
                    <span>个人中心</span>
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>
                    <span>退出登录</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <router-link class="btn-login" to="/login">登录</router-link>
            <router-link class="btn-primary" to="/register">注册</router-link>
          </template>
        </div>
      </div>
    </header>

    <main class="user-main" :class="{ chat: isChatPage }">
      <router-view />
    </main>

    <footer v-if="!isChatPage" class="user-footer">
      <p>{{ systemStore.displayName }} · 用户中心</p>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Bell, Calendar, ChatDotRound, Document, HomeFilled, Notebook, SwitchButton, Tickets, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useSystemStore } from '@/stores/system'
import { applyUserPalette, restoreAdminTheme } from '@/stores/theme'
import { mediaUrl } from '@/utils/format'
import BrandLogo from '@/components/BrandLogo.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const systemStore = useSystemStore()
const scrolled = ref(false)
const isChatPage = computed(() => route.path.startsWith('/user/assistant'))

function isNavActive(path) {
  if (path === '/user/home') return route.path === '/user/home'
  return route.path === path || route.path.startsWith(`${path}/`)
}

const navItems = [
  { path: '/user/home', title: '首页', icon: HomeFilled },
  { path: '/user/scores', title: '成绩', icon: Tickets },
  { path: '/user/timetable', title: '开课安排', icon: Calendar },
  { path: '/user/transcript', title: '成绩单', icon: Document },
  { path: '/user/notices', title: '公告', icon: Bell },
  { path: '/user/assistant', title: '校园助手', icon: ChatDotRound },
  { path: '/user/profile', title: '个人中心', icon: User }
]

const displayName = computed(() => userStore.user?.nickname || userStore.user?.username || '用户')
const avatarSrc = computed(() => mediaUrl(userStore.user?.avatar))
const avatarFallback = computed(() => displayName.value.slice(0, 1).toUpperCase())
const isLoggedIn = computed(() => !!userStore.token)

function onScroll() {
  scrolled.value = window.scrollY > 8
}

function onCommand(cmd) {
  if (cmd === 'home') router.push('/user/home')
  if (cmd === 'scores') router.push('/user/scores')
  if (cmd === 'assistant') router.push('/user/assistant')
  if (cmd === 'plans') router.push('/user/plans')
  if (cmd === 'profile') router.push('/user/profile')
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/user/home')
  }
}

onMounted(async () => {
  applyUserPalette()
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
  if (userStore.token && !userStore.user) {
    try {
      await userStore.fetchUser()
    } catch {
      userStore.logout()
    }
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  restoreAdminTheme()
})
</script>

<style scoped>
.user-shell {
  --ink: #1a1a1a;
  --muted: #8a8f98;
  --line: #eceef2;
  --accent: #e63935;
  --accent-soft: rgba(230, 57, 53, 0.08);
  --bg: #f7f8fa;
  min-height: 100vh;
  color: var(--ink);
  background: var(--bg);
  font-family: 'DM Sans', 'Noto Sans SC', sans-serif;
}

.user-nav {
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid transparent;
  backdrop-filter: blur(14px);
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
}

.user-nav.scrolled {
  border-bottom-color: var(--line);
  box-shadow: 0 8px 24px rgba(26, 26, 26, 0.04);
}

.nav-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 12px 28px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  color: var(--ink);
  flex-shrink: 0;
  transition: opacity 0.2s ease;
}

.brand:hover {
  opacity: 0.92;
}

.brand-logo {
  width: 42px;
  height: 42px;
  display: block;
  filter: drop-shadow(0 6px 14px rgba(230, 57, 53, 0.28));
}

.nav-brand-logo {
  filter: drop-shadow(0 6px 14px rgba(230, 57, 53, 0.28));
}

.brand-logo svg {
  width: 100%;
  height: 100%;
  display: block;
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.brand-name {
  font-size: 18px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0.01em;
}

.brand-tagline {
  font-size: 12px;
  color: var(--muted);
  line-height: 1.2;
  white-space: nowrap;
}

.nav-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  flex: 1;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 999px;
  color: #4b5563;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: color 0.2s ease, background 0.2s ease, transform 0.2s ease;
}

.nav-link:hover {
  color: var(--ink);
  background: rgba(26, 26, 26, 0.04);
}

.nav-link.router-link-active {
  color: var(--accent);
  background: var(--accent-soft);
  font-weight: 600;
}

.nav-link :deep(.el-icon) {
  transition: transform 0.2s ease;
}

.nav-link:hover :deep(.el-icon) {
  transform: translateY(-1px);
}

.nav-actions {
  margin-left: auto;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.btn-login,
.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  padding: 0 18px;
  border-radius: 999px;
  text-decoration: none;
  font-size: 14px;
  font-weight: 600;
  transition: transform 0.2s ease, background 0.2s ease, border-color 0.2s ease;
}

.btn-login {
  color: var(--ink);
  background: #fff;
  border: 1px solid var(--line);
}

.btn-login:hover {
  border-color: #dddfe5;
  transform: translateY(-1px);
}

.btn-primary {
  color: #fff;
  background: var(--accent);
  border: 1px solid var(--accent);
}

.btn-primary:hover {
  background: #cf2f2c;
  border-color: #cf2f2c;
  transform: translateY(-1px);
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 4px 12px 4px 4px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.user-chip:hover {
  border-color: #dddfe5;
  box-shadow: 0 6px 18px rgba(26, 26, 26, 0.06);
  transform: translateY(-1px);
}

.user-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 1px;
  min-width: 0;
  text-align: left;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  max-width: 108px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-role {
  font-size: 11px;
  color: var(--muted);
}

.chev {
  color: #9ca3af;
  margin-right: 2px;
}

.user-main {
  min-height: calc(100vh - 148px);
}

.user-shell.chat {
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.user-main.chat {
  flex: 1;
  min-height: 0;
  height: auto;
  overflow: hidden;
}

.user-footer {
  padding: 28px;
  text-align: center;
  color: var(--muted);
  font-size: 13px;
  border-top: 1px solid var(--line);
  background: #fff;
}

.user-footer p {
  margin: 0;
}

@media (max-width: 900px) {
  .nav-inner {
    padding: 12px 16px;
    gap: 14px;
  }

  .brand-tagline {
    display: none;
  }

  .nav-links {
    justify-content: flex-start;
    overflow-x: auto;
  }

  .user-meta {
    display: none;
  }

  .chev {
    display: none;
  }

  .user-chip {
    padding: 3px;
  }
}

@media (max-width: 640px) {
  .nav-link span {
    display: none;
  }

  .nav-link {
    padding: 10px 12px;
  }
}
</style>
```

---


## 第 15 步：助手页面 Assistant.vue（完整）

**文件：** `frontend/src/views/user/Assistant.vue`

用 `fetch` 调 `/api/user/edu/agent/chat/stream`（不要用 Axios 缓冲 SSE）。

```vue
<template>
  <div class="assistant">
    <section class="stage">
      <header class="stage-head">
        <div class="stage-title">
          <span class="mark" aria-hidden="true" />
          <div>
            <h1>校园助手</h1>
            <p>先弄清你的问题，再去查成绩、课表、公告或校规，用真实数据回答</p>
          </div>
        </div>
        <div class="stage-actions">
          <el-button v-if="messages.length" text class="new-chat" :disabled="sending" @click="startNewChat">新对话</el-button>
          <span class="mode" :class="{ llm: lastUsedLlm }">{{ lastUsedLlm ? '模型回答' : '本地编排' }}</span>
        </div>
      </header>

      <div ref="scroller" class="thread">
        <div v-if="!messages.length" class="empty">
          <div class="empty-hero">
            <div class="empty-icon">
              <el-icon :size="28"><ChatDotRound /></el-icon>
            </div>
            <h2>你好，{{ displayName }}</h2>
            <p>
              我不是随便闲聊，而是会先判断你要查什么，再去系统里取成绩、课表、公告或校规资料，最后整理成回答。
              个人数据以教务系统为准，不会凭空编造；问校规时也会尽量附上参考来源。
              也可以让我按课表制定学习计划，生成后说「保存」即可同步到学习计划页。
            </p>
          </div>
          <div class="prompts">
            <button
              v-for="item in prompts"
              :key="item.text"
              type="button"
              class="prompt"
              :class="item.tone"
              @click="send(item.text)"
            >
              <span class="prompt-top">
                <span class="prompt-icon"><el-icon :size="16"><component :is="item.icon" /></el-icon></span>
                <span class="prompt-tag">{{ item.tag }}</span>
              </span>
              <strong>{{ item.title }}</strong>
              <span class="prompt-text">{{ item.text }}</span>
            </button>
          </div>
        </div>

        <div v-else class="board">
          <article
            v-for="(item, index) in messages"
            :key="item.id || `${item.role}-${index}`"
            class="turn"
            :class="item.role"
          >
            <div class="who">
              <span class="avatar" :class="item.role === 'user' ? 'me' : 'bot'">
                <img v-if="item.role === 'user' && userAvatarSrc" :src="userAvatarSrc" alt="" />
                <template v-else>{{ item.role === 'user' ? avatarFallback : '助' }}</template>
              </span>
              <strong>{{ item.role === 'user' ? displayName : '校园助手' }}</strong>
            </div>

            <div class="bubble">
              <div v-if="processOf(item).length" class="process">
                <p v-for="(line, i) in processOf(item)" :key="i" class="process-line" :class="line.phase">
                  <span>{{ line.phase === 'done' ? '完成' : '开始' }}：{{ line.name }}</span>
                  <span v-if="line.output" class="process-out">{{ shortOut(line.output) }}</span>
                </p>
              </div>

              <p v-if="item.streaming" class="live">{{ item.content }}<i class="caret" /></p>
              <template v-else>
              <template v-for="(block, bIndex) in blocks(item)" :key="bIndex">
                <p v-if="block.type === 'label'" class="label">{{ block.text }}</p>
                <p v-else-if="block.type === 'text'">{{ block.text }}</p>
                <dl v-else-if="block.type === 'stats'" class="facts">
                  <div v-for="fact in block.items" :key="fact.label" class="fact">
                    <dt>{{ fact.label }}</dt>
                    <dd>{{ fact.value }}</dd>
                  </div>
                </dl>
                <div v-else-if="block.type === 'scores'" class="score-block">
                  <section v-for="group in block.groups" :key="group.semester">
                    <p class="group-title">{{ group.semester }}</p>
                    <div class="sheet-wrap">
                      <table class="sheet">
                        <thead>
                          <tr>
                            <th>课程</th>
                            <th class="num">学分</th>
                            <th class="num">平时</th>
                            <th class="num">期末</th>
                            <th class="num">总评</th>
                            <th class="num">绩点</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="(row, rIndex) in group.rows" :key="rIndex">
                            <td>{{ row.course }}</td>
                            <td class="num">{{ row.credit }}</td>
                            <td class="num">{{ row.usual }}</td>
                            <td class="num">{{ row.exam }}</td>
                            <td class="num emphasis">{{ row.total }}</td>
                            <td class="num emphasis">{{ row.point }}</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </section>
                </div>
                <div v-else-if="block.type === 'table'" class="sheet-wrap">
                  <table class="sheet">
                    <thead>
                      <tr>
                        <th>时间</th>
                        <th>节次</th>
                        <th>课程</th>
                        <th>教师</th>
                        <th>教室</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, rIndex) in block.rows" :key="rIndex">
                        <td>{{ row.week }}</td>
                        <td>{{ row.section }}</td>
                        <td>{{ row.course }}</td>
                        <td>{{ row.teacher }}</td>
                        <td>{{ row.room }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <div v-else-if="block.type === 'plan'" class="sheet-wrap">
                  <table class="sheet">
                    <thead>
                      <tr>
                        <th>时间</th>
                        <th>课程</th>
                        <th>时长</th>
                        <th>安排</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, rIndex) in block.rows" :key="rIndex">
                        <td>{{ row.week }}</td>
                        <td>{{ row.course }}</td>
                        <td class="num">{{ row.minutes }}分钟</td>
                        <td>{{ row.task }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </template>
              </template>

              <div v-if="uniqueCites(item).length" class="sources">
                <span class="sources-label">参考资料</span>
                <article v-for="cite in uniqueCites(item)" :key="cite.title" class="source">
                  <b>《{{ cite.title }}》</b>
                  <span>{{ cite.snippet }}</span>
                </article>
              </div>
            </div>
          </article>
        </div>
      </div>

      <form class="composer" @submit.prevent="send(draft)">
        <el-input
          v-model="draft"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          resize="none"
          placeholder="问问成绩、课表、学习计划或补考，Enter 发送"
          @keydown.enter.exact.prevent="send(draft)"
        />
        <el-button type="primary" class="send" :loading="sending" native-type="submit" :disabled="!draft.trim()">
          发送
        </el-button>
      </form>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { Calendar, ChatDotRound, Document, Notebook, TrendCharts } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import { useUserStore } from '@/stores/user'
import { mediaUrl } from '@/utils/format'

const userStore = useUserStore()
const displayName = computed(() => userStore.user?.nickname || userStore.user?.username || '同学')
const avatarFallback = computed(() => displayName.value.slice(0, 1))
const userAvatarSrc = computed(() => mediaUrl(userStore.user?.avatar))

const prompts = [
  { title: '我的绩点', text: '我的累计绩点是多少？', tag: '成绩', tone: 'gpa', icon: TrendCharts },
  { title: '本周学习计划', text: '帮我制定本周学习计划', tag: '计划', tone: 'plan', icon: Notebook },
  { title: '补考规则', text: '不及格了怎么补考？', tag: '校规', tone: 'rule', icon: Document },
  { title: '本学期课表', text: '本学期课表是什么？', tag: '课表', tone: 'table', icon: Calendar }
]
const messages = ref([])
const sessionId = ref(null)
const draft = ref('')
const sending = ref(false)
const lastUsedLlm = ref(false)
const scroller = ref()

const TIME_LINE = /^(周[一二三四五六日])\s*第(\d+)\s*-\s*(\d+)节\s+(.+?)\s*\/\s*(.+?)\s*\/\s*(.+)$/
const SCORE_LINE = /^(.+?)\s+(\S+)\s+学分([\d.]+)\s+平时([\d.]+)\s+期末([\d.]+)\s+总评([\d.]+)\s+绩点([\d.]+)$/
const PLAN_LINE = /^【计划】(周[一二三四五六日])\s+(\S+)\s+(\d+)分钟\s+(.+)$/

function uniqueCites(item) {
  const list = item?.citations || []
  const seen = new Set()
  return list.filter((cite) => {
    const key = cite.title
    if (!key || seen.has(key)) return false
    seen.add(key)
    return true
  })
}

function linesOf(item) {
  const cites = uniqueCites(item)
  const titles = new Set(cites.map((cite) => cite.title))
  return String(item?.content || '')
    .split(/\n+/)
    .map((line) => line.trim())
    .filter((line) => {
      if (!line) return false
      if (cites.length && /和问题最接近的校内资料/.test(line)) return false
      if (cites.length && Array.from(titles).some((title) => line.includes(`《${title}》`))) return false
      return true
    })
}

function stripRankText(text) {
  return String(text || '')
    .replace(/【排名】/g, '【绩点】')
    .replace(/，?\s*班级排名\s*[^，。\n]*/g, '')
    .replace(/，?\s*专业排名\s*[^，。\n]*/g, '')
    .replace(/，?\s*成绩排名\s*[^，。\n]*/g, '')
    .replace(/，?\s*名次\s*[^，。\n]*/g, '')
    .replace(/，{2,}/g, '，')
    .replace(/^\s*，|，\s*$/g, '')
    .trim()
}

function parseFacts(line) {
  return stripRankText(line)
    .replace(/^【绩点】/, '')
    .replace(/。$/, '')
    .split('，')
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) => {
      const idx = part.indexOf(' ')
      if (idx < 0) return null
      const label = part.slice(0, idx)
      if (/排名|名次/.test(label)) return null
      return { label, value: part.slice(idx + 1) }
    })
    .filter(Boolean)
}

function groupScores(rows) {
  const groups = []
  const map = new Map()
  for (const row of rows) {
    if (!map.has(row.semester)) {
      const group = { semester: row.semester, rows: [] }
      map.set(row.semester, group)
      groups.push(group)
    }
    map.get(row.semester).rows.push(row)
  }
  return groups
}

function blocks(item) {
  const slots = []
  let table = []
  let scores = []
  let plans = []
  const flushTable = () => {
    if (!table.length) return
    slots.push({ type: 'table', rows: table })
    table = []
  }
  const flushScores = () => {
    if (!scores.length) return
    slots.push({ type: 'scores', groups: groupScores(scores) })
    scores = []
  }
  const flushPlans = () => {
    if (!plans.length) return
    slots.push({ type: 'plan', rows: plans })
    plans = []
  }
  const flush = () => {
    flushTable()
    flushScores()
    flushPlans()
  }
  for (const raw of linesOf(item)) {
    const line = stripRankText(raw)
    if (!line) continue
    // 整行只剩排名相关时丢弃；含其他信息的已在 stripRankText 中去掉排名片段
    if (/^(班级|专业|成绩)?排名/.test(line) || /^名次/.test(line)) continue
    const time = line.match(TIME_LINE)
    if (time) {
      flushScores()
      flushPlans()
      table.push({
        week: time[1],
        section: `第${time[2]}-${time[3]}节`,
        course: time[4].trim(),
        teacher: time[5].trim(),
        room: time[6].trim()
      })
      continue
    }
    const score = line.match(SCORE_LINE)
    if (score) {
      flushTable()
      flushPlans()
      scores.push({
        semester: score[1].trim(),
        course: score[2].trim(),
        credit: score[3],
        usual: score[4],
        exam: score[5],
        total: score[6],
        point: score[7]
      })
      continue
    }
    const plan = line.match(PLAN_LINE)
    if (plan) {
      flushTable()
      flushScores()
      plans.push({
        week: plan[1],
        course: plan[2],
        minutes: plan[3],
        task: plan[4].trim()
      })
      continue
    }
    if (line.startsWith('【绩点】')) {
      flush()
      const items = parseFacts(line)
      if (items.length) slots.push({ type: 'stats', items })
      continue
    }
    if (/^【(课表|我的成绩|绩点|排名)】$/.test(line)) continue
    const section = line.match(/^【([^】]+)】(.*)$/)
    if (section) {
      flush()
      const heading = section[1].trim()
      if (/排名|名次/.test(heading)) continue
      slots.push({ type: 'label', text: heading })
      const rest = section[2].trim()
      if (rest) slots.push({ type: 'text', text: rest })
      continue
    }
    flush()
    slots.push({ type: 'text', text: line })
  }
  flush()
  if (!slots.length) slots.push({ type: 'text', text: item?.content || '' })
  return slots
}

async function loadConversation() {
  const sessions = await request.get('/user/edu/agent/sessions')
  const latest = Array.isArray(sessions) && sessions.length ? sessions[0] : null
  if (!latest?.id) {
    sessionId.value = null
    messages.value = []
    return
  }
  sessionId.value = latest.id
  messages.value = await request.get(`/user/edu/agent/sessions/${latest.id}/messages`)
  lastUsedLlm.value = messages.value.slice().reverse().find((item) => item.role === 'assistant')?.usedLlm || false
  await scrollBottom()
}

function processOf(item) {
  if (item?.traces?.length) return item.traces
  if (item?.role === 'user') return []
  return (item?.steps || []).map((name) => ({ phase: 'done', name, output: '' }))
}

function shortOut(text) {
  const value = String(text || '').replace(/\s+/g, ' ').trim()
  if (!value) return ''
  return value.length > 140 ? `${value.slice(0, 140)}…` : value
}

async function startNewChat() {
  if (sending.value) return
  if (messages.value.length) {
    await ElMessageBox.confirm('开启新对话后，当前页面会清空。后台仍可查看历史会话。', '新对话', {
      type: 'info',
      confirmButtonText: '开始新对话',
      cancelButtonText: '取消'
    })
  }
  const data = await request.post('/user/edu/agent/sessions')
  sessionId.value = data?.id || null
  messages.value = []
  lastUsedLlm.value = false
  ElMessage.success('已开启新对话')
}

function applyEvent(reply, data) {
  if (data.type === 'trace') {
    reply.traces.push({ phase: data.phase, name: data.name, output: data.output || '' })
    return
  }
  if (data.type === 'delta') {
    reply.content += data.text || ''
    return
  }
  if (data.type === 'reset') {
    reply.content = ''
    return
  }
  if (data.type === 'done') {
    sessionId.value = data.sessionId ?? sessionId.value
    reply.usedLlm = !!data.usedLlm
    reply.citations = data.citations || []
    lastUsedLlm.value = reply.usedLlm
    return
  }
  if (data.type === 'error') {
    throw new Error(data.message || '助手暂时无法回答')
  }
}

async function send(text) {
  const message = String(text || '').trim()
  if (!message || sending.value) return
  messages.value.push({ role: 'user', content: message })
  draft.value = ''
  sending.value = true
  const reply = reactive({
    role: 'assistant',
    content: '',
    traces: [],
    citations: [],
    streaming: true,
    usedLlm: false
  })
  messages.value.push(reply)
  await scrollBottom()
  try {
    const token = localStorage.getItem('token')
    const headers = { 'Content-Type': 'application/json' }
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }
    const response = await fetch('/api/user/edu/agent/chat/stream', {
      method: 'POST',
      headers,
      body: JSON.stringify({ sessionId: sessionId.value, message })
    })
    const contentType = response.headers.get('content-type') || ''
    if (!response.ok || !contentType.includes('text/event-stream') || !response.body) {
      const err = await response.json().catch(() => ({}))
      throw new Error(err.message || '助手暂时无法回答')
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const chunks = buffer.split('\n\n')
      buffer = chunks.pop() || ''
      for (const chunk of chunks) {
        const line = chunk.split('\n').find((row) => row.startsWith('data:'))
        if (!line) continue
        applyEvent(reply, JSON.parse(line.slice(5).trim()))
      }
      await scrollBottom()
    }
  } catch (error) {
    if (!reply.content && !reply.traces.length) {
      messages.value.splice(messages.value.length - 2, 2)
    }
    ElMessage.error(error.message || '助手暂时无法回答')
  } finally {
    reply.streaming = false
    sending.value = false
    await scrollBottom()
  }
}

async function scrollBottom() {
  await nextTick()
  if (scroller.value) scroller.value.scrollTop = scroller.value.scrollHeight
}

onMounted(loadConversation)
</script>

<style scoped>
.assistant {
  --ink: #1a1a1a;
  --muted: #6b7280;
  --line: #eceef2;
  --accent: #e63935;
  --user: #2a3340;
  --sky: #3d6f99;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: var(--ink);
  padding: 16px 20px 20px;
  box-sizing: border-box;
  background:
    radial-gradient(680px 280px at 12% -18%, rgba(230, 57, 53, 0.1), transparent 62%),
    radial-gradient(560px 240px at 96% 0%, rgba(230, 57, 53, 0.05), transparent 55%),
    #f7f8fa;
}

.stage {
  width: min(960px, 100%);
  margin: 0 auto;
  height: 100%;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid #eceef2;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(26, 35, 48, 0.05);
  overflow: hidden;
}

.stage-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 24px;
  flex-shrink: 0;
  border-bottom: 1px solid #f0f1f4;
  box-sizing: border-box;
  background:
    linear-gradient(100deg, rgba(230, 57, 53, 0.07), transparent 42%),
    #fff;
}

.stage-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mark {
  width: 4px;
  height: 36px;
  border-radius: 4px;
  background: linear-gradient(180deg, #f0716d, var(--accent));
}

.stage-head h1 {
  margin: 0;
  font-size: 22px;
  letter-spacing: -0.03em;
  font-weight: 700;
}

.stage-head p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.stage-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.new-chat {
  color: #667085 !important;
  font-weight: 600;
}

.new-chat:hover {
  color: var(--accent) !important;
}

.mode {
  flex-shrink: 0;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid #eceef2;
  color: #6b7280;
  font-size: 12px;
  font-weight: 650;
  background: #f7f8fa;
}

.mode.llm {
  color: #1f6b55;
  border-color: rgba(31, 107, 85, 0.18);
  background: rgba(31, 107, 85, 0.08);
}

.thread {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 14px 0 12px;
}

.board {
  display: flex;
  flex-direction: column;
  gap: 22px;
  padding: 2px 22px 10px;
  box-sizing: border-box;
}

.turn {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
  animation: rise 0.25s ease;
}

.turn.user {
  align-items: flex-end;
}

@keyframes rise {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

.who {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--muted);
  font-size: 12px;
}

.turn.user .who {
  flex-direction: row-reverse;
}

.who strong {
  font-weight: 600;
  color: #4b5563;
}

.avatar {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 700;
  overflow: hidden;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar.bot {
  color: #fff;
  background: linear-gradient(145deg, #f0716d, var(--accent));
  box-shadow: 0 4px 10px rgba(230, 57, 53, 0.28);
}

.avatar.me {
  color: #fff;
  background: linear-gradient(145deg, #4a5868, var(--user));
}

.bubble {
  width: 100%;
  padding: 16px 18px;
  border-radius: 16px;
  border: 1px solid var(--line);
  background: #fff;
}

.turn.user .bubble {
  width: fit-content;
  max-width: min(520px, 78%);
  color: #fff;
  background: linear-gradient(155deg, #455466, var(--user));
  border-color: transparent;
  border-bottom-right-radius: 6px;
}

.turn.assistant .bubble {
  border-bottom-left-radius: 6px;
  border-color: rgba(230, 57, 53, 0.14);
  box-shadow: 0 8px 22px rgba(26, 35, 48, 0.04);
}

.bubble p {
  margin: 0 0 8px;
  line-height: 1.7;
  word-break: break-word;
}

.bubble p:last-child {
  margin-bottom: 0;
}

.bubble .label,
.group-title {
  margin: 0 0 12px;
  padding-left: 10px;
  border-left: 3px solid var(--accent);
  font-weight: 700;
  color: var(--ink);
}

.group-title {
  margin-bottom: 10px !important;
  font-size: 13px;
  color: #2d3745;
}

.turn.user .bubble .label {
  color: #fff;
  border-left-color: rgba(255, 255, 255, 0.55);
}

.sheet-wrap {
  overflow: auto;
  margin: 2px 0 12px;
  border: 1px solid var(--line);
  border-radius: 12px;
}

.sheet {
  width: 100%;
  border-collapse: collapse;
  min-width: 480px;
}

.sheet th,
.sheet td {
  padding: 11px 12px;
  text-align: left;
  font-size: 13px;
  border-bottom: 1px solid #e8ebf0;
}

.sheet tr:last-child td {
  border-bottom: 0;
}

.sheet tbody tr:nth-child(even) td {
  background: rgba(61, 111, 153, 0.035);
}

.sheet th {
  color: #5f6b7a;
  font-weight: 650;
  white-space: nowrap;
  border-bottom-color: #dde2ea;
  background: linear-gradient(180deg, #fbfcfe, #f4f6f9);
}

.sheet .num {
  text-align: right;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  color: #3d4756;
}

.sheet .emphasis {
  font-weight: 700;
  color: var(--accent);
}

.score-block section + section {
  margin-top: 16px;
}

.facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin: 4px 0 12px;
  border: 1px solid var(--line);
  border-radius: 12px;
  overflow: hidden;
}

.fact {
  position: relative;
  padding: 12px 14px;
  border-right: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}

.fact::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: 2px;
  background: linear-gradient(180deg, #f07a6e, var(--sky));
  opacity: 0.75;
}

.fact:nth-child(3n) {
  border-right: 0;
}

.facts .fact:nth-last-child(-n + 3) {
  border-bottom: 0;
}

.fact dt,
.fact dd {
  margin: 0;
  padding-left: 8px;
}

.fact dt {
  color: var(--muted);
  font-size: 11px;
  margin-bottom: 5px;
}

.fact dd {
  font-size: 14px;
  font-weight: 700;
  line-height: 1.35;
  word-break: break-word;
  color: var(--ink);
}

.fact:nth-child(4) dd,
.fact:nth-child(5) dd {
  color: var(--accent);
}

.empty {
  width: min(720px, 100%);
  margin: 28px auto 0;
  padding: 0 24px 8px;
  box-sizing: border-box;
}

.empty-hero {
  text-align: center;
  margin-bottom: 22px;
}

.empty-icon {
  width: 58px;
  height: 58px;
  margin: 0 auto 14px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  color: #fff;
  background: linear-gradient(145deg, #f0716d, var(--accent));
  box-shadow: 0 10px 22px rgba(230, 57, 53, 0.24);
}

.empty-hero h2 {
  margin: 0;
  font-size: 28px;
  letter-spacing: -0.03em;
}

.empty-hero p {
  margin: 10px auto 0;
  max-width: 440px;
  color: var(--muted);
  line-height: 1.7;
  font-size: 14px;
}

.prompts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.prompt {
  position: relative;
  text-align: left;
  padding: 16px 16px 15px;
  border: 1px solid #eceef2;
  border-radius: 16px;
  background: #fff;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.prompt::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--accent);
  opacity: 0.55;
}

.prompt:hover {
  border-color: rgba(230, 57, 53, 0.35);
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(26, 35, 48, 0.07);
  background: linear-gradient(160deg, #fff, #fff8f7);
}

.prompt-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.prompt-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 9px;
  color: #fff;
  background: var(--accent);
}

.prompt-tag {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 650;
  color: var(--accent);
  background: rgba(230, 57, 53, 0.08);
}

.prompt.gpa .prompt-icon,
.prompt.gpa::before {
  background: #e63935;
}

.prompt.plan .prompt-icon,
.prompt.plan::before {
  background: #d97706;
}

.prompt.plan .prompt-tag {
  color: #b45309;
  background: rgba(217, 119, 6, 0.1);
}

.prompt.rule .prompt-icon,
.prompt.rule::before {
  background: #2563eb;
}

.prompt.rule .prompt-tag {
  color: #1d4ed8;
  background: rgba(37, 99, 235, 0.1);
}

.prompt.table .prompt-icon,
.prompt.table::before {
  background: #0f766e;
}

.prompt.table .prompt-tag {
  color: #0f766e;
  background: rgba(15, 118, 110, 0.1);
}

.prompt strong {
  display: block;
  margin-bottom: 6px;
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
}

.prompt-text {
  display: block;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
}

.sources {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--line);
}

.sources-label {
  display: block;
  margin-bottom: 8px;
  color: var(--muted);
  font-size: 12px;
}

.source {
  padding: 10px 0;
  border-bottom: 1px solid var(--line);
}

.source:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.source b,
.source span {
  display: block;
}

.source b {
  margin-bottom: 4px;
  font-size: 13px;
}

.source span {
  color: var(--muted);
  font-size: 12px;
  line-height: 1.55;
}

.process {
  margin: 0 0 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f4f6f8;
  border: 1px solid #e6eaef;
  font-size: 12px;
  line-height: 1.55;
  color: #3d4a5c;
}

.process-line {
  margin: 0 0 8px;
}

.process-line:last-child {
  margin-bottom: 0;
}

.process-line.start {
  color: #3d6f99;
}

.process-out {
  display: block;
  margin-top: 4px;
  color: #667085;
  white-space: pre-wrap;
  word-break: break-word;
}

.live {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.7;
}

.caret {
  display: inline-block;
  width: 7px;
  height: 1em;
  margin-left: 2px;
  vertical-align: text-bottom;
  background: var(--accent);
  animation: blink 0.9s step-end infinite;
}

@keyframes blink {
  50% { opacity: 0; }
}

.composer {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  width: calc(100% - 48px);
  margin: 0 24px 18px;
  padding: 12px 12px 12px 16px;
  border: 1px solid #eceef2;
  border-radius: 16px;
  flex-shrink: 0;
  box-sizing: border-box;
  background: #fafbfc;
}

.composer:focus-within {
  border-color: rgba(230, 57, 53, 0.4);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(230, 57, 53, 0.08);
}

.composer :deep(.el-textarea__inner) {
  box-shadow: none;
  border: 0;
  padding: 8px 4px;
  background: transparent;
  color: var(--ink);
}

.send {
  height: 40px;
  min-width: 76px;
  border-radius: 12px;
  font-weight: 650;
  background: var(--accent) !important;
  border-color: var(--accent) !important;
}

.send:hover {
  background: #cf2f2c !important;
  border-color: #cf2f2c !important;
}

@media (max-width: 900px) {
  .assistant {
    padding: 10px 12px 12px;
  }

  .stage {
    border-radius: 16px;
  }

  .stage-head,
  .board,
  .empty {
    padding-left: 14px;
    padding-right: 14px;
  }

  .composer {
    width: calc(100% - 28px);
    margin: 0 14px 14px;
  }

  .facts {
    grid-template-columns: 1fr 1fr;
  }

  .fact:nth-child(3n) {
    border-right: 1px solid var(--line);
  }

  .fact:nth-child(2n) {
    border-right: 0;
  }

  .facts .fact:nth-last-child(-n + 3) {
    border-bottom: 1px solid var(--line);
  }

  .facts .fact:nth-last-child(-n + 2) {
    border-bottom: 0;
  }

  .prompts {
    grid-template-columns: 1fr;
  }

  .turn.user .bubble {
    max-width: 88%;
  }
}
</style>
```

---

## 第 16 步：启动前端并验证

```bash
cd frontend
npm install
npm run dev
```

打开 `http://127.0.0.1:5173/login`，用 `linxiao` / `123456` 登录。

| 试问 | 期望 |
| --- | --- |
| 查一下我的成绩 | 过程区出现查询成绩；正文是真实分数 |
| 看一下我的课表 | 出现课表工具；可问「今天的课」 |
| 最近有什么公告 | 出现公告列表 |
| 教室借用与调课说明 | RAG 命中知识库摘录 |
| 帮我制定学习计划 | 生成计划；再说「保存」写入学习计划页 |

---

## 常见问题

| 现象 | 处理 |
| --- | --- |
| 助手只有本地编排、不走模型 | 未配 `DEEPSEEK_API_KEY`，或问题走了成绩/课表/公告本地路径（正常） |
| Spring AI ChatClient 为空 | Boot ≥3.4，检查 `spring-ai-starter-model-openai` 与 `spring.ai.openai.api-key` |
| 流式卡住/一次性蹦出 | 检查 `vite.config.js` 对 stream 的 `cache-control` / `x-accel-buffering` |
| 404 /user/assistant | 是否已写入 `router/index.js` 并重启/刷新前端 |
| 侧栏没有校园助手 | 检查 `UserLayout.vue` 的 `navItems` |

---

文档与仓库代码同步：粘贴时请以 `springboot-vue-scaffold` 内源文件为准。
