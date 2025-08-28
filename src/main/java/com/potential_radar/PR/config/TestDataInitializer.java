package com.potential_radar.PR.config;

import com.potential_radar.PR.tech.domain.TechPart;
import com.potential_radar.PR.tech.domain.TechStack;
import com.potential_radar.PR.tech.repository.TechPartRepository;
import com.potential_radar.PR.tech.repository.TechStackRepository;
import com.potential_radar.PR.user.domain.*;
import com.potential_radar.PR.user.repository.UserRepository;
import com.potential_radar.PR.user.repository.UserProfileRepository;
import com.potential_radar.PR.user.repository.UserTechStackRepository;
import com.potential_radar.PR.project.domain.*;
import com.potential_radar.PR.project.repository.*;
import com.potential_radar.PR.recommendation.repository.RecommendationHistoryRepository;
import com.potential_radar.PR.invitation.repository.TeamInvitationRepository;
import com.potential_radar.PR.like.domain.Like;
import com.potential_radar.PR.like.domain.TargetType;
import com.potential_radar.PR.like.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test & false") // 테스트 환경에서는 실행하지 않음
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final TechPartRepository techPartRepository;
    private final TechStackRepository techStackRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectTechStackRepository projectTechStackRepository;
    private final ProjectTechPartRepository projectTechPartRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final RecommendationHistoryRepository recommendationHistoryRepository;
    private final UserTechStackRepository userTechStackRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final LikeRepository likeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 이미 사용자 테스트 데이터가 있는지 확인
        boolean hasUserData = userRepository.findByEmail("user001@naver.com").isPresent();
        // TechStack 데이터가 있는지 확인 (새로 추가된 데이터)
        boolean hasTechStackData = techStackRepository.count() > 20; // 기본 데이터보다 많으면 초기화된 것으로 간주
        // 프로젝트 데이터가 있는지 확인
        boolean hasProjectData = projectRecruitmentRepository.count() > 0;

        log.info("=== Initializing seed data (idempotent for ddl-auto:update) ===");
        
        // 0. recommendation_history 테이블 구조 업데이트 (먼저 실행)
        updateRecommendationHistoryTable();
        
        // 1. TechPart 데이터 생성
        initializeTechParts();
        
        // 2. TechStack 데이터 생성 (항상 실행하여 새 데이터 추가)
        if (!hasTechStackData) {
            initializeTechStacks();
        } else {
            log.info("TechStack data already exists, skipping TechStack initialization");
        }
        
        // 3. 100명의 사용자 데이터 생성
        if (!hasUserData) {
            initializeUsers();
        } else {
            log.info("User data already exists, skipping user initialization");
        }
        
        // 5. 사용자 기술스택 데이터 생성 (AI 추천을 위해) - 매칭 개선을 위해 항상 재생성
        boolean hasUserTechStackData = userTechStackRepository.count() > 0;
        if (hasUserTechStackData) {
            log.info("Clearing existing user tech stack data for better project matching...");
            userTechStackRepository.deleteAll();
            log.info("Deleted existing user tech stacks");
        }
        initializeUserTechStacks();
        
        // 4. 프로젝트 데이터 생성 (기술스택 매칭 개선을 위해 항상 재생성)
        // 기존 프로젝트 데이터 삭제 (외래키 제약조건 고려하여 순서대로)
        if (hasProjectData) {
            log.info("Clearing existing project data for tech stack improvement...");
            // 1단계: 추천 피드백 먼저 삭제 (외래키 참조 제거)
            try {
                jdbcTemplate.execute("DELETE FROM recommendation_feedback");
                log.info("Deleted recommendation feedbacks");
            } catch (Exception e) {
                log.warn("Failed to delete recommendation feedbacks: {}", e.getMessage());
            }
            
            // 2단계: 추천 이력 삭제
            recommendationHistoryRepository.deleteAll();
            log.info("Deleted recommendation histories");
            
            // 3단계: 팀 초대 삭제 (team_invitations)
            teamInvitationRepository.deleteAll();
            log.info("Deleted team invitations");
            
            // 4단계: 프로젝트 멤버 삭제 (project_member)
            projectMemberRepository.deleteAll();
            log.info("Deleted project members");
            
            // 5단계: 프로젝트 지원 삭제 (project_application)
            projectApplicationRepository.deleteAll();
            log.info("Deleted project applications");
            
            // 6단계: 프로젝트 관련 연결 테이블 삭제
            projectTechStackRepository.deleteAll();
            projectTechPartRepository.deleteAll();
            log.info("Deleted project relations");
            
            // 7단계: 프로젝트 삭제
            projectRecruitmentRepository.deleteAll();
            log.info("Deleted projects");
        }
        initializeProjects();
        
        // 6. 스마트한 좋아요 데이터 생성 (프로젝트 데이터 생성 후)
        boolean hasLikeData = likeRepository.count() > 0;
        if (!hasLikeData) {
            initializeLikeData();
        } else {
            log.info("Like data already exists, skipping like initialization");
        }
        
        log.info("Test data initialization completed successfully!");
    }

    private void initializeTechParts() {
        List<String> techPartNames = Arrays.asList(
            "프론트엔드", "백엔드", "풀스택", "모바일", "데브옵스", 
            "데이터사이언스", "AI/ML", "게임개발", "보안", "QA/테스터", 
            "UI/UX디자인", "PM/기획"
        );

        for (String name : techPartNames) {
            if (techPartRepository.findByNameIgnoreCase(name).isEmpty()) {
                TechPart techPart = TechPart.builder()
                    .name(name)
                    .build();
                techPartRepository.save(techPart);
            }
        }
    }

    private void initializeTechStacks() {
        List<String> techStackNames = Arrays.asList(
            // Frontend 기술
            "React", "Vue.js", "Angular", "Next.js", "Nuxt.js", "Svelte", "SvelteKit",
            "JavaScript", "TypeScript", "HTML5", "CSS3", "SCSS", "Sass", "Less",
            "Styled Components", "Emotion", "Tailwind CSS", "Bootstrap", "Material-UI",
            "Ant Design", "Chakra UI", "Webpack", "Vite", "Parcel", "Rollup",
            "ESLint", "Prettier", "Jest", "Cypress", "Playwright", "React Testing Library", "Storybook",

            // Backend 기술
            "Spring Boot", "Spring Framework", "Spring Security", "Spring Data JPA", "Spring Cloud",
            "Node.js", "Express.js", "Nest.js", "Fastify", "Django", "Flask", "FastAPI",
            "ASP.NET Core", "ASP.NET", "Laravel", "Symfony", "CodeIgniter", "Ruby on Rails",
            "Sinatra", "Phoenix", "Gin", "Echo", "Fiber", "Actix Web", "Rocket",
            "Ktor", "Quarkus", "Micronaut", "Helidon",

            // 프로그래밍 언어
            "Java", "Python", "C#", "C++", "C", "Go", "Rust", "Kotlin", "Scala", "Clojure",
            "Ruby", "PHP", "Swift", "Objective-C", "Dart", "R", "MATLAB", "Perl",
            "Haskell", "Elixir", "Erlang", "F#", "VB.NET", "COBOL", "Fortran",
            "Assembly", "Bash", "PowerShell",

            // 데이터베이스
            "PostgreSQL", "MySQL", "MariaDB", "SQLite", "Oracle Database", "SQL Server",
            "MongoDB", "Redis", "Elasticsearch", "Cassandra", "DynamoDB", "CouchDB",
            "Neo4j", "InfluxDB", "TimescaleDB", "Firebase Firestore", "Supabase",
            "PlanetScale", "Neon", "CockroachDB", "Amazon RDS", "Amazon Aurora",
            "Google Cloud SQL", "Azure SQL Database",

            // Cloud & DevOps
            "AWS", "AWS EC2", "AWS S3", "AWS RDS", "AWS Lambda", "AWS ECS", "AWS EKS",
            "AWS CloudFormation", "AWS CDK", "AWS API Gateway", "AWS CloudFront",
            "AWS Route 53", "AWS VPC", "AWS IAM", "AWS SQS", "AWS SNS", "AWS EventBridge",
            "Amazon ElastiCache", "Amazon OpenSearch", "Google Cloud Platform",
            "Google Cloud Run", "Google Cloud Functions", "Google Kubernetes Engine",
            "Google Cloud Storage", "Google BigQuery", "Firebase", "Azure",
            "Azure App Service", "Azure Functions", "Azure Kubernetes Service",
            "Azure Cosmos DB", "Azure Storage", "Docker", "Kubernetes", "Helm",
            "Terraform", "Ansible", "Chef", "Puppet", "Vagrant", "Jenkins",
            "GitLab CI/CD", "GitHub Actions", "Azure DevOps", "CircleCI", "Travis CI",
            "Bitbucket Pipelines", "ArgoCD", "Flux", "Prometheus", "Grafana",
            "ELK Stack", "Fluentd", "Logstash", "Kibana", "Jaeger", "Zipkin",
            "New Relic", "Datadog", "Sentry", "Consul", "Vault", "Nomad",
            "Istio", "Envoy", "NGINX", "Apache HTTP Server", "Traefik", "HAProxy", "Cloudflare",

            // Mobile Development
            "React Native", "Flutter", "Ionic", "Xamarin", "Cordova", "SwiftUI",
            "UIKit", "Android SDK", "Kotlin Multiplatform", "Unity", "Unreal Engine",

            // AI/ML & Data Science
            "TensorFlow", "PyTorch", "Keras", "Scikit-learn", "Pandas", "NumPy",
            "Matplotlib", "Seaborn", "Plotly", "Jupyter", "Anaconda", "Apache Spark",
            "Apache Kafka", "Apache Airflow", "MLflow", "Kubeflow", "ONNX", "OpenCV",
            "Hugging Face", "LangChain", "OpenAI API", "Anthropic Claude", "Cohere",

            // Testing & Quality
            "JUnit", "TestNG", "Mockito", "Selenium", "Cucumber", "Postman", "Insomnia",
            "k6", "Artillery", "Apache JMeter", "SonarQube", "Checkstyle", "SpotBugs", "PMD",

            // Version Control & Collaboration
            "Git", "GitHub", "GitLab", "Bitbucket", "Azure Repos", "SVN", "Mercurial",
            "Jira", "Confluence", "Slack", "Microsoft Teams", "Discord", "Notion",
            "Trello", "Asana", "Linear", "Monday.com",

            // Design & UI/UX
            "Figma", "Adobe XD", "Sketch", "InVision", "Zeplin", "Adobe Photoshop",
            "Adobe Illustrator", "Canva", "Framer", "Principle",

            // Development Tools & IDEs
            "Visual Studio Code", "IntelliJ IDEA", "Eclipse", "NetBeans", "Xcode",
            "Android Studio", "PyCharm", "WebStorm", "PhpStorm", "RubyMine",
            "GoLand", "CLion", "DataGrip", "Rider", "Visual Studio", "Vim",
            "Emacs", "Sublime Text", "Atom", "Brackets",

            // Web Technologies
            "GraphQL", "REST API", "gRPC", "WebSocket", "Socket.io", "OAuth 2.0",
            "JWT", "OpenAPI", "Swagger", "Progressive Web App", "Service Worker",
            "WebAssembly", "WebRTC",

            // Game Development
            "Unity 3D", "Unreal Engine 4", "Unreal Engine 5", "Godot", "GameMaker Studio",
            "Construct 3", "Phaser", "Three.js", "Babylon.js", "OpenGL", "DirectX", "Vulkan",

            // Blockchain & Web3
            "Ethereum", "Solidity", "Web3.js", "Ethers.js", "Hardhat", "Truffle",
            "Polygon", "Binance Smart Chain", "Solana", "Cardano", "Polkadot",
            "Hyperledger", "IPFS",

            // IoT & Embedded
            "Arduino", "Raspberry Pi", "ESP32", "ESP8266", "STM32", "FreeRTOS",
            "Zephyr", "Mbed", "PlatformIO", "MQTT", "CoAP", "LoRaWAN",

            // Security
            "OWASP", "Burp Suite", "Metasploit", "Nmap", "Wireshark", "Kali Linux",
            "Penetration Testing", "Ethical Hacking", "SSL/TLS", "PKI", "OAuth",
            "SAML", "LDAP", "Active Directory",

            // Virtualization & Containers
            "VMware", "VirtualBox", "QEMU", "Proxmox", "Hyper-V", "Docker Compose",
            "Docker Swarm", "Podman", "LXC", "OpenShift", "Rancher",

            // Monitoring & Observability
            "Nagios", "Zabbix", "Cacti", "PRTG", "SolarWinds", "Splunk",
            "Elastic APM", "Application Insights", "CloudWatch", "Stackdriver",

            // Networking
            "TCP/IP", "HTTP/HTTPS", "DNS", "BGP", "OSPF", "VLAN", "VPN", "SDN",
            "OpenFlow", "Cisco", "Juniper", "Palo Alto", "Fortinet",

            // Business Intelligence
            "Tableau", "Power BI", "QlikView", "Looker", "Metabase", "Apache Superset",
            "D3.js", "Chart.js", "Highcharts", "Google Analytics", "Adobe Analytics",

            // CMS & E-commerce
            "WordPress", "Drupal", "Joomla", "Magento", "Shopify", "WooCommerce",
            "PrestaShop", "OpenCart", "BigCommerce", "Strapi", "Contentful",
            "Sanity", "Ghost", "Headless CMS",

            // Message Queues & Event Streaming
            "RabbitMQ", "Apache ActiveMQ", "Amazon SQS", "Google Pub/Sub",
            "Azure Service Bus", "NATS", "ZeroMQ", "Pulsar"
        );

        log.info("Initializing TechStack data...");
        int count = 0;
        for (String name : techStackNames) {
            if (techStackRepository.findByNameIgnoreCase(name).isEmpty()) {
                TechStack techStack = TechStack.builder()
                    .name(name)
                    .build();
                techStackRepository.save(techStack);
                count++;
            }
        }
        log.info("Created {} new TechStack entries", count);
    }

    private void initializeUsers() {
        List<TechPart> techParts = techPartRepository.findAll();
        Random random = new Random();
        String hashedPassword = passwordEncoder.encode("password123"); // 공통 비밀번호

        String[] nicknames = {
            "코딩마스터001", "개발자김철수", "프론트엔드박영희", "백엔드이민수", "풀스택홍길동",
            "ReactDeveloper", "SpringMaster", "NodeJSExpert", "VueDeveloper", "PythonGuru",
            "JavaScriptNinja", "SQLMaster", "CSSWizard", "HTMLCoder", "TypeScriptDev",
            "AndroidDev", "iOSExpert", "FlutterCoder", "ReactNativeDev", "SwiftProgrammer",
            "KotlinDev", "DevOpsEngineer", "DockerMaster", "KubernetesExpert", "AWSSpecialist",
            "DataScientist", "MLEngineer", "AIResearcher", "BigDataAnalyst", "TensorFlowDev",
            "GameDeveloper", "UnityExpert", "UnrealCoder", "GameDesigner", "GraphicsProgrammer",
            "SecurityExpert", "PenetrationTester", "CybersecurityAnalyst", "SecurityArchitect", "CryptographyExpert",
            "QAEngineer", "TestAutomation", "PerformanceTester", "ManualTester", "QualityAssurance",
            "UIDesigner", "UXResearcher", "ProductDesigner", "InteractionDesigner", "VisualDesigner",
            "ProductManager", "ProjectManager", "TechLead", "ScrumMaster", "BusinessAnalyst",
            "SystemArchitect", "DatabaseAdmin", "NetworkEngineer", "CloudArchitect", "InfraEngineer",
            "JavaDeveloper", "CSharpDev", "GoDeveloper", "RustProgrammer", "PhpDeveloper",
            "RubyDeveloper", "ScalaDeveloper", "ElixirCoder", "HaskellDev", "ClojureCoder",
            "BlockchainDev", "SmartContractDev", "Web3Developer", "DAppDeveloper", "NFTCreator",
            "IoTDeveloper", "EmbeddedEngineer", "FirmwareDev", "HardwareEngineer", "RoboticsEngineer",
            "ARDeveloper", "VRDeveloper", "XREngineer", "MetaverseDev", "3DGraphicsDev",
            "QuantumComputing", "CloudNativeDev", "MicroservicesArch", "ServerlessExpert", "EdgeComputingDev",
            "TechEvangelist", "DeveloperAdvocate", "TechnicalWriter", "CodeReviewer", "OpenSourceMaintainer",
            "CommunityManager", "TechRecruiter", "StartupFounder", "TechConsultant", "DigitalNomad"
        };

        Provider[] providers = {Provider.EMAIL, Provider.GOOGLE, Provider.KAKAO};
        ExperienceRange[] experienceRanges = ExperienceRange.values();

        for (int i = 0; i < 100; i++) {
            final int userIndex = i;
            int userNum = i + 1;
            String email = String.format("user%03d@naver.com", userNum);
            final Provider finalProvider = (i % 5 < 3) ? Provider.EMAIL : (i % 5 == 3 ? Provider.GOOGLE : Provider.KAKAO);

            // User 생성 (upsert 방식)
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                // 닉네임 중복 체크 및 고유 닉네임 생성
                String baseNickname = nicknames[userIndex];
                String uniqueNickname = baseNickname;
                int suffix = 1;
                
                while (userRepository.findByNickname(uniqueNickname).isPresent()) {
                    uniqueNickname = baseNickname + "_" + suffix;
                    suffix++;
                }
                
                User newUser = User.builder()
                    .email(email)
                    .password(hashedPassword)
                    .nickname(uniqueNickname)
                    .provider(finalProvider)
                    .providerUserId(finalProvider != Provider.EMAIL ? 
                        finalProvider.name().toLowerCase() + "_" + (random.nextInt(900000000) + 100000000) : null)
                    .profileImage(null)
                    .build();
                return userRepository.save(newUser);
            });

            // UserProfile 생성
            TechPart techPart = techParts.get(i % techParts.size());
            
            UserProfile userProfile = UserProfile.builder()
                .user(user)
                .techPart(techPart)
                .bio(i % 3 == 0 ? null : 
                    (i % 5 == 0 ? 
                        "안녕하세요! " + nicknames[i] + "입니다. 다양한 프로젝트 경험을 통해 성장하고 있습니다." :
                        "열정적인 개발자 " + nicknames[i] + "입니다. 새로운 기술 학습과 협업을 좋아합니다. 함께 멋진 프로젝트를 만들어봅시다!"))
                .jobTitle(i % 4 == 0 ? null : getJobTitleByTechPart(techPart.getName()))
                .phone(i % 5 == 0 ? null : 
                    String.format("010-%04d-%04d", (userNum * 37) % 10000, (userNum * 73) % 10000))
                .githubUrl(i % 3 == 0 ? null : 
                    "https://github.com/" + nicknames[i].toLowerCase().replaceAll("[^a-z0-9]", ""))
                .linkedinUrl(i % 4 == 0 ? null : 
                    "https://linkedin.com/in/" + nicknames[i].toLowerCase().replaceAll("[^a-z0-9]", "-"))
                .websiteUrl(i % 6 == 0 ? null : 
                    "https://" + nicknames[i].toLowerCase().replaceAll("[^a-z0-9]", "") + ".dev")
                .isPortfolioOpen(i % 3 == 1)
                .isContactOpen(i % 4 == 1)
                .isSearchOpen(i % 2 == 1)
                .reputationScore(BigDecimal.valueOf(Math.round((random.nextDouble() * 4.5 + 0.5) * 100.0) / 100.0))
                .reviewCount(random.nextInt(51))
                .experienceRange(experienceRanges[i % experienceRanges.length])
                .build();

            userProfileRepository.save(userProfile);

            if ((i + 1) % 20 == 0) {
                log.info("Created {} users...", i + 1);
            }
        }
    }

    private String getJobTitleByTechPart(String techPartName) {
        return switch (techPartName) {
            case "프론트엔드" -> "Frontend Developer";
            case "백엔드" -> "Backend Developer";
            case "풀스택" -> "Full Stack Developer";
            case "모바일" -> "Mobile Developer";
            case "데브옵스" -> "DevOps Engineer";
            case "데이터사이언스" -> "Data Scientist";
            case "AI/ML" -> "AI/ML Engineer";
            case "게임개발" -> "Game Developer";
            case "보안" -> "Security Expert";
            case "QA/테스터" -> "QA Engineer";
            case "UI/UX디자인" -> "UI/UX Designer";
            case "PM/기획" -> "Product Manager";
            default -> "Software Developer";
        };
    }

    private void initializeProjects() {
        log.info("Initializing project data...");
        
        List<User> users = userRepository.findAll();
        List<TechPart> techParts = techPartRepository.findAll();
        List<TechStack> techStacks = techStackRepository.findAll();
        Random random = new Random();
        
        if (users.isEmpty() || techParts.isEmpty() || techStacks.isEmpty()) {
            log.warn("Required data not found. Users: {}, TechParts: {}, TechStacks: {}", 
                users.size(), techParts.size(), techStacks.size());
            return;
        }

        String[] projectTitles = {
            "AI 기반 개인 맞춤형 학습 플랫폼 개발",
            "실시간 협업 화이트보드 웹 애플리케이션",
            "블록체인 기반 탈중앙화 소셜 미디어",
            "IoT 스마트 홈 자동화 시스템",
            "머신러닝을 활용한 주식 투자 분석 도구",
            "모바일 AR 쇼핑 경험 애플리케이션",
            "클라우드 기반 팀 프로젝트 관리 도구",
            "실시간 언어 번역 화상 회의 서비스",
            "게임화된 온라인 코딩 교육 플랫폼",
            "환경 데이터 모니터링 및 분석 시스템",
            "NFT 기반 디지털 아트 마켓플레이스",
            "음성 인식 기반 AI 비서 앱",
            "실시간 건강 모니터링 웨어러블 연동 앱",
            "지속가능한 에너지 관리 스마트 그리드",
            "VR 가상 여행 체험 플랫폼",
            "자율주행차 시뮬레이션 및 테스트 환경",
            "개인화된 뉴스 큐레이션 AI 서비스",
            "온라인 멘토링 매칭 플랫폼",
            "실시간 재난 알림 및 대피 안내 시스템",
            "크라우드펀딩 기반 스타트업 지원 플랫폼",
            "AI 기반 개인 영양사 및 식단 관리 앱",
            "실시간 주차 공간 찾기 및 예약 서비스",
            "블록체인 기반 투명한 기부 추적 시스템",
            "음악 AI 작곡 및 편집 도구",
            "스마트 시티 교통 최적화 솔루션",
            "온라인 의료 상담 및 처방 플랫폼",
            "게임 스트리밍 및 커뮤니티 플랫폼",
            "AI 기반 법률 자문 챗봇 서비스",
            "실시간 농작물 모니터링 스마트팜",
            "가상현실 기반 원격 교육 시스템",
            "React 기반 소셜 네트워킹 플랫폼",
            "Spring Boot 마이크로서비스 아키텍처",
            "Flutter 크로스플랫폼 모바일 앱",
            "Vue.js 기반 전자상거래 사이트",
            "Django REST API 백엔드 서버",
            "Angular 기반 대시보드 시스템",
            "Node.js 실시간 채팅 애플리케이션",
            "Python 데이터 분석 플랫폼",
            "Java 기반 엔터프라이즈 솔루션",
            "TypeScript 기반 프로젝트 관리 도구",
            "Go 언어 기반 고성능 API 서버",
            "Rust 기반 시스템 프로그래밍 도구",
            "Unity 3D 인디 게임 개발",
            "React Native 모바일 커머스 앱",
            "Kotlin Android 네이티브 앱",
            "Swift iOS 소셜 미디어 앱",
            "Docker 컨테이너 기반 DevOps 플랫폼",
            "Kubernetes 클러스터 관리 시스템",
            "PostgreSQL 기반 데이터베이스 설계",
            "MongoDB NoSQL 문서 관리 시스템"
        };

        String[] projectDescriptions = {
            "개인의 학습 패턴을 분석하여 최적화된 학습 경로를 제공하는 AI 플랫폼입니다.",
            "실시간으로 여러 사용자가 함께 협업할 수 있는 디지털 화이트보드를 구현합니다.",
            "사용자의 프라이버시를 보장하면서 탈중앙화된 소셜 네트워킹을 제공합니다.",
            "IoT 센서와 AI를 활용하여 스마트 홈 환경을 자동으로 제어하는 시스템입니다.",
            "딥러닝 알고리즘을 사용하여 주식 시장 동향을 예측하고 투자 전략을 제안합니다.",
            "AR 기술을 활용하여 실제 공간에서 가상 제품을 체험할 수 있는 쇼핑 앱입니다.",
            "팀 프로젝트의 전 과정을 효율적으로 관리할 수 있는 클라우드 기반 도구입니다.",
            "실시간 언어 번역 기능이 포함된 화상 회의 솔루션을 개발합니다.",
            "게임 요소를 접목하여 재미있게 프로그래밍을 학습할 수 있는 교육 플랫폼입니다.",
            "환경 센서 데이터를 수집하고 분석하여 환경 변화를 모니터링하는 시스템입니다.",
            "NFT 기술을 활용한 디지털 아트 작품 거래 및 전시 플랫폼을 구축합니다.",
            "자연어 처리 기술을 활용한 스마트 AI 비서 애플리케이션을 개발합니다.",
            "웨어러블 기기와 연동하여 실시간으로 건강 상태를 모니터링하는 앱입니다.",
            "재생 에너지를 효율적으로 관리하고 배분하는 스마트 그리드 시스템입니다.",
            "VR 기술을 활용하여 집에서 세계 각지를 여행할 수 있는 체험을 제공합니다.",
            "자율주행 알고리즘을 안전하게 테스트할 수 있는 가상 환경을 구축합니다.",
            "사용자의 관심사와 성향을 분석하여 맞춤형 뉴스를 제공하는 AI 서비스입니다.",
            "전문가와 학습자를 연결하는 온라인 멘토링 매칭 플랫폼을 구현합니다.",
            "재난 발생 시 실시간으로 알림을 제공하고 최적의 대피 경로를 안내합니다.",
            "혁신적인 아이디어를 가진 스타트업과 투자자를 연결하는 플랫폼입니다.",
            "개인의 건강 상태와 선호도를 고려한 맞춤형 식단을 추천하는 AI 앱입니다.",
            "실시간 주차 공간 정보를 제공하고 미리 예약할 수 있는 서비스입니다.",
            "기부금의 사용 내역을 투명하게 추적할 수 있는 블록체인 시스템입니다.",
            "AI 기술을 활용하여 자동으로 음악을 작곡하고 편집할 수 있는 도구입니다.",
            "도시 교통 흐름을 실시간으로 분석하고 최적화하는 스마트 시티 솔루션입니다.",
            "원격으로 의료진과 상담하고 처방받을 수 있는 온라인 의료 플랫폼입니다.",
            "게임 스트리밍과 게이머 커뮤니티 기능을 통합한 종합 플랫폼입니다.",
            "AI 기술을 활용하여 법률 질문에 대한 기초적인 자문을 제공하는 챗봇입니다.",
            "센서와 AI를 활용하여 농작물의 생육 상태를 실시간으로 모니터링합니다.",
            "VR 기술을 활용하여 몰입감 있는 원격 교육 환경을 제공하는 시스템입니다.",
            "React와 Node.js를 활용한 현대적인 소셜 네트워킹 플랫폼을 구축합니다.",
            "Spring Boot 기반의 확장 가능한 마이크로서비스 아키텍처를 설계합니다.",
            "Flutter를 사용하여 iOS와 Android에서 동시에 작동하는 모바일 앱을 개발합니다.",
            "Vue.js와 최신 프론트엔드 기술을 활용한 전자상거래 웹사이트를 구현합니다.",
            "Django REST Framework로 확장성 있는 백엔드 API 서버를 구축합니다.",
            "Angular를 활용한 반응형 관리자 대시보드 시스템을 개발합니다.",
            "Node.js와 Socket.io를 사용한 실시간 멀티채널 채팅 서비스를 구현합니다.",
            "Python과 Pandas, NumPy를 활용한 빅데이터 분석 플랫폼을 구축합니다.",
            "Java와 Spring 생태계를 활용한 대규모 엔터프라이즈 솔루션을 개발합니다.",
            "TypeScript의 타입 안정성을 활용한 프로젝트 관리 도구를 구현합니다.",
            "Go 언어의 동시성을 활용한 고성능 REST API 서버를 개발합니다.",
            "Rust의 메모리 안전성을 활용한 시스템 레벨 프로그래밍 도구를 구축합니다.",
            "Unity 3D 엔진을 활용한 3D 인디 게임을 개발하고 스팀에 출시합니다.",
            "React Native로 크로스플랫폼 모바일 커머스 애플리케이션을 구현합니다.",
            "Kotlin을 사용한 Android 네이티브 앱으로 최적화된 사용자 경험을 제공합니다.",
            "Swift와 SwiftUI를 활용한 iOS 전용 소셜 미디어 앱을 개발합니다.",
            "Docker 컨테이너 기술을 활용한 CI/CD 파이프라인과 DevOps 플랫폼을 구축합니다.",
            "Kubernetes를 활용한 컨테이너 오케스트레이션 및 클러스터 관리 시스템을 구현합니다.",
            "PostgreSQL의 고급 기능을 활용한 확장성 있는 데이터베이스 아키텍처를 설계합니다.",
            "MongoDB의 유연성을 활용한 NoSQL 기반 문서 관리 및 검색 시스템을 구축합니다."
        };

        for (int i = 0; i < 50; i++) {
            // 팀 리더 선택 (규칙적 분산: user001~005가 각각 10개씩)
            int teamLeaderIndex = i / 10; // 0-9: user001, 10-19: user002, 20-29: user003, 30-39: user004, 40-49: user005
            User teamLeader = users.get(teamLeaderIndex);
            
            // 프로젝트 생성
            LocalDate today = LocalDate.now();
            LocalDate recruitDeadline = today.plusDays(random.nextInt(30) + 10); // 10-40일 후
            LocalDate startDate = recruitDeadline.plusDays(random.nextInt(14) + 1); // 모집 마감 후 1-14일
            LocalDate endDate = startDate.plusDays(random.nextInt(120) + 30); // 시작일 후 30-150일
            
            ProjectRecruitment project = ProjectRecruitment.builder()
                .teamLeader(teamLeader)
                .title(projectTitles[i % projectTitles.length])
                .description(projectDescriptions[i % projectDescriptions.length])
                .recruitDeadline(recruitDeadline)
                .startDate(startDate)
                .endDate(endDate)
                .status(ProjectStatus.values()[random.nextInt(ProjectStatus.values().length)])
                .viewCount(random.nextInt(500))
                .recruitCount(random.nextInt(5) + 3)
                .build();


            // save 이전에 createdAt 값을 직접 설정
            project.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(365)));


            final ProjectRecruitment savedProject = projectRecruitmentRepository.save(project);
            
            // 프로젝트 기술 파트 연결 (1-3개 랜덤 선택)
            int techPartCount = random.nextInt(3) + 1;
            List<TechPart> selectedTechParts = new java.util.ArrayList<>();
            
            for (int j = 0; j < techPartCount; j++) {
                TechPart techPart;
                do {
                    techPart = techParts.get(random.nextInt(techParts.size()));
                } while (selectedTechParts.contains(techPart));
                
                selectedTechParts.add(techPart);
                
                ProjectTechPart projectTechPart = ProjectTechPart.builder()
                    .project(savedProject)
                    .techPart(techPart)
                    .recruitCount(random.nextInt(3) + 1) // 각 파트별 1-3명 모집
                    .build();
                
                projectTechPartRepository.save(projectTechPart);
            }
            
            // 프로젝트 기술 스택 연결 (프로젝트별 적절한 기술스택 선택)
            List<String> projectTechStackNames = getProjectTechStacks(i, projectTitles[i % projectTitles.length]);
            
            for (String techStackName : projectTechStackNames) {
                techStackRepository.findByNameIgnoreCase(techStackName).ifPresent(techStack -> {
                    ProjectTechStack projectTechStack = ProjectTechStack.builder()
                        .project(savedProject)
                        .techStack(techStack)
                        .recruitCount(random.nextInt(2) + 1) // 각 기술스택별 1-2명 모집
                        .build();
                    
                    projectTechStackRepository.save(projectTechStack);
                });
            }
            
            if ((i + 1) % 10 == 0) {
                log.info("Created {} projects...", i + 1);
            }
        }
        
        log.info("Successfully created 50 projects with related tech parts and tech stacks");
    }
    
    /**
     * 프로젝트별로 적절한 기술스택을 반환합니다
     */
    private List<String> getProjectTechStacks(int projectIndex, String projectTitle) {
        return switch (projectIndex % 50) {
            case 0 -> Arrays.asList("React", "TypeScript", "Node.js", "Python", "PostgreSQL", "Docker");
            case 1 -> Arrays.asList("React", "JavaScript", "Node.js", "Express.js", "Socket.io", "MongoDB");
            case 2 -> Arrays.asList("Vue.js", "JavaScript", "Python", "Django", "PostgreSQL", "Redis");
            case 3 -> Arrays.asList("Java", "Spring Boot", "Python", "PostgreSQL", "Docker", "AWS");
            case 4 -> Arrays.asList("Python", "Django", "React", "TypeScript", "PostgreSQL", "TensorFlow");
            case 5 -> Arrays.asList("React Native", "TypeScript", "Node.js", "MongoDB", "AWS");
            case 6 -> Arrays.asList("Angular", "TypeScript", "Java", "Spring Boot", "PostgreSQL", "Docker");
            case 7 -> Arrays.asList("React", "TypeScript", "Python", "FastAPI", "PostgreSQL", "Docker");
            case 8 -> Arrays.asList("Flutter", "Dart", "Firebase", "Node.js", "MongoDB");
            case 9 -> Arrays.asList("Python", "NumPy", "Pandas", "Scikit-learn", "PostgreSQL", "Docker");
            case 10 -> Arrays.asList("React", "TypeScript", "Solidity", "Web3.js", "MongoDB");
            case 11 -> Arrays.asList("Python", "TensorFlow", "React", "Node.js", "PostgreSQL");
            case 12 -> Arrays.asList("React Native", "TypeScript", "Firebase", "AWS");
            case 13 -> Arrays.asList("Java", "Spring Boot", "React", "PostgreSQL", "Docker", "Kubernetes");
            case 14 -> Arrays.asList("Unity", "C#", "Blender", "Firebase");
            case 15 -> Arrays.asList("Python", "Selenium", "React", "Node.js", "PostgreSQL");
            case 16 -> Arrays.asList("React", "TypeScript", "Python", "TensorFlow", "PostgreSQL");
            case 17 -> Arrays.asList("Angular", "TypeScript", "Java", "Spring Boot", "PostgreSQL");
            case 18 -> Arrays.asList("Python", "Django", "React", "PostgreSQL", "AWS");
            case 19 -> Arrays.asList("React", "JavaScript", "Node.js", "MongoDB", "AWS");
            case 20 -> Arrays.asList("Python", "TensorFlow", "React", "Node.js", "MongoDB");
            case 21 -> Arrays.asList("React", "TypeScript", "Node.js", "PostgreSQL", "Redis");
            case 22 -> Arrays.asList("Solidity", "Web3.js", "React", "Node.js", "MongoDB");
            case 23 -> Arrays.asList("Python", "TensorFlow", "React", "Node.js", "PostgreSQL");
            case 24 -> Arrays.asList("Java", "Spring Boot", "React", "PostgreSQL", "AWS");
            case 25 -> Arrays.asList("React", "TypeScript", "Node.js", "PostgreSQL", "Docker");
            case 26 -> Arrays.asList("Unity", "C#", "React", "Node.js", "MongoDB");
            case 27 -> Arrays.asList("Python", "TensorFlow", "React", "PostgreSQL", "Docker");
            case 28 -> Arrays.asList("Java", "Spring Boot", "Python", "PostgreSQL", "Docker");
            case 29 -> Arrays.asList("Unity", "C#", "Blender", "Node.js", "MongoDB");
            case 30 -> Arrays.asList("React", "TypeScript", "Node.js", "MongoDB", "Docker");
            case 31 -> Arrays.asList("Java", "Spring Boot", "PostgreSQL", "Docker", "Kubernetes");
            case 32 -> Arrays.asList("Flutter", "Dart", "Firebase", "MongoDB");
            case 33 -> Arrays.asList("Vue.js", "JavaScript", "Node.js", "PostgreSQL", "Docker");
            case 34 -> Arrays.asList("Python", "Django", "PostgreSQL", "Redis", "Docker");
            case 35 -> Arrays.asList("Angular", "TypeScript", "Java", "PostgreSQL", "Docker");
            case 36 -> Arrays.asList("Node.js", "JavaScript", "Socket.io", "MongoDB", "Redis");
            case 37 -> Arrays.asList("Python", "Pandas", "NumPy", "PostgreSQL", "Docker");
            case 38 -> Arrays.asList("Java", "Spring Boot", "PostgreSQL", "Docker", "AWS");
            case 39 -> Arrays.asList("TypeScript", "React", "Node.js", "PostgreSQL", "Docker");
            case 40 -> Arrays.asList("Go", "PostgreSQL", "Redis", "Docker", "Kubernetes");
            case 41 -> Arrays.asList("Rust", "PostgreSQL", "Docker", "Linux");
            case 42 -> Arrays.asList("Unity", "C#", "Blender", "Firebase");
            case 43 -> Arrays.asList("React Native", "TypeScript", "Firebase", "MongoDB");
            case 44 -> Arrays.asList("Kotlin", "Android", "Firebase", "SQLite");
            case 45 -> Arrays.asList("Swift", "iOS", "Firebase", "CoreData");
            case 46 -> Arrays.asList("Docker", "Kubernetes", "Jenkins", "AWS", "Linux");
            case 47 -> Arrays.asList("Kubernetes", "Docker", "Helm", "AWS", "Linux");
            case 48 -> Arrays.asList("PostgreSQL", "Docker", "pgAdmin", "AWS");
            case 49 -> Arrays.asList("MongoDB", "Node.js", "Express.js", "Docker");
            default -> Arrays.asList("JavaScript", "Node.js", "MongoDB");
        };
    }
    
    /**
     * 사용자 기술스택 데이터 초기화
     * 각 사용자에게 3-7개의 기술스택을 랜덤으로 할당
     */
    private void initializeUserTechStacks() {
        log.info("Initializing user tech stack data...");
        
        List<User> users = userRepository.findAll();
        List<TechStack> techStacks = techStackRepository.findAll();
        Random random = new Random();
        
        if (users.isEmpty() || techStacks.isEmpty()) {
            log.warn("Required data not found. Users: {}, TechStacks: {}", 
                users.size(), techStacks.size());
            return;
        }
        
        // 프로젝트에서 많이 사용되는 핵심 기술스택들 (대소문자 구분 없이)
        String[] commonTechNames = {
            "React", "TypeScript", "JavaScript", "Node.js", "Python", "Java", "Spring Boot", 
            "PostgreSQL", "MongoDB", "Docker", "AWS", "Vue.js", "Angular", "Django", 
            "FastAPI", "Express.js", "MySQL", "Redis", "Flutter", "React Native",
            "Kubernetes", "TensorFlow", "Unity 3D", "HTML", "CSS", "Git", "Linux"
        };
        
        // 핵심 기술스택들을 DB에서 찾기
        List<TechStack> commonTechStacks = new java.util.ArrayList<>();
        List<TechStack> otherTechStacks = new java.util.ArrayList<>();
        
        for (TechStack techStack : techStacks) {
            boolean isCommon = false;
            for (String commonName : commonTechNames) {
                if (techStack.getName().equalsIgnoreCase(commonName)) {
                    isCommon = true;
                    break;
                }
            }
            
            if (isCommon) {
                commonTechStacks.add(techStack);
            } else {
                otherTechStacks.add(techStack);
            }
        }
        
        log.info("Found {} common tech stacks out of {} total", commonTechStacks.size(), techStacks.size());
        
        for (User user : users) {
            // 각 사용자에게 3-6개의 기술스택 할당
            int techStackCount = random.nextInt(4) + 3; // 3-6개
            List<TechStack> selectedTechStacks = new java.util.ArrayList<>();
            
            // 80% 확률로 핵심 기술스택 선택, 20% 확률로 기타 기술스택 선택
            for (int i = 0; i < techStackCount; i++) {
                TechStack techStack;
                int maxAttempts = 10; // 무한루프 방지
                int attempts = 0;
                
                do {
                    attempts++;
                    if (random.nextDouble() < 0.8 && !commonTechStacks.isEmpty()) {
                        // 80% 확률로 핵심 기술스택에서 선택
                        techStack = commonTechStacks.get(random.nextInt(commonTechStacks.size()));
                    } else if (!otherTechStacks.isEmpty()) {
                        // 20% 확률로 기타 기술스택에서 선택
                        techStack = otherTechStacks.get(random.nextInt(otherTechStacks.size()));
                    } else {
                        // 기타 기술스택이 없으면 전체에서 선택
                        techStack = techStacks.get(random.nextInt(techStacks.size()));
                    }
                    
                    if (attempts >= maxAttempts) {
                        break; // 무한루프 방지
                    }
                } while (selectedTechStacks.contains(techStack));
                
                if (!selectedTechStacks.contains(techStack)) {
                    selectedTechStacks.add(techStack);
                    
                    // UserTechStack 생성 (핵심 기술스택은 높은 레벨로)
                    int skillLevel;
                    if (commonTechStacks.contains(techStack)) {
                        // 핵심 기술스택은 3-5 레벨
                        skillLevel = random.nextInt(3) + 3;
                    } else {
                        // 기타 기술스택은 1-4 레벨
                        skillLevel = random.nextInt(4) + 1;
                    }
                    
                    UserTechStack userTechStack = UserTechStack.builder()
                        .user(user)
                        .stack(techStack)
                        .skillLevel(skillLevel)
                        .build();
                    
                    userTechStackRepository.save(userTechStack);
                }
            }
        }
        
        long totalUserTechStacks = userTechStackRepository.count();
        log.info("Successfully created {} user tech stack relationships for {} users", 
            totalUserTechStacks, users.size());
    }
    
    /**
     * recommendation_history 테이블 구조 업데이트
     * 추천 타입과 프로젝트 컨텍스트 ID 컬럼 추가
     */
    private void updateRecommendationHistoryTable() {
        try {
            log.info("Updating recommendation_history table structure...");
            
            // 1. recommendation_type 컬럼 추가 (이미 있으면 무시)
            jdbcTemplate.execute("""
                ALTER TABLE recommendation_history 
                ADD COLUMN IF NOT EXISTS recommendation_type VARCHAR(10) DEFAULT 'PROJECT'
                """);
            
            // 2. project_context_id 컬럼 추가 (이미 있으면 무시)
            jdbcTemplate.execute("""
                ALTER TABLE recommendation_history 
                ADD COLUMN IF NOT EXISTS project_context_id BIGINT
                """);
            
            // 3. 기존 데이터에 대한 타입 설정
            jdbcTemplate.update("""
                UPDATE recommendation_history 
                SET recommendation_type = 'PROJECT' 
                WHERE recommended_project_id IS NOT NULL AND recommendation_type IS NULL
                """);
            
            jdbcTemplate.update("""
                UPDATE recommendation_history 
                SET recommendation_type = 'MEMBER' 
                WHERE recommended_user_id IS NOT NULL AND recommendation_type IS NULL
                """);
            
            // 4. 인덱스 추가 (이미 있으면 무시)
            try {
                jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_recommendation_history_type 
                    ON recommendation_history(recommendation_type)
                    """);
                    
                jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_recommendation_history_user_type 
                    ON recommendation_history(user_id, recommendation_type)
                    """);
                    
                jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_recommendation_history_project_context 
                    ON recommendation_history(project_context_id)
                    """);
            } catch (Exception e) {
                // 인덱스 생성 실패는 무시 (이미 존재하거나 DB가 지원하지 않을 수 있음)
                log.debug("Index creation failed (may already exist): {}", e.getMessage());
            }
            
            log.info("✅ recommendation_history 테이블 구조 업데이트 완료");
            
        } catch (Exception e) {
            log.warn("⚠️ recommendation_history 테이블 구조 업데이트 실패: {}", e.getMessage());
            log.debug("상세 에러:", e);
            // 테이블 구조 업데이트 실패가 전체 초기화를 방해하지 않도록 예외를 던지지 않음
        }
    }
    
    /**
     * 스마트한 좋아요 데이터 생성
     * 사용자의 기술스택과 프로젝트의 기술스택 유사도를 기반으로 현실적인 좋아요 패턴 생성
     */
    private void initializeLikeData() {
        log.info("Initializing smart like data based on tech stack similarity...");
        
        List<User> users = userRepository.findAll();
        List<ProjectRecruitment> projects = projectRecruitmentRepository.findAll();
        Random random = new Random();
        
        if (users.isEmpty() || projects.isEmpty()) {
            log.warn("Required data not found. Users: {}, Projects: {}", 
                users.size(), projects.size());
            return;
        }
        
        // 성능 최적화: 좋아요는 처음 20명 사용자만 생성
        List<User> likeUsers = users.size() > 20 ? users.subList(0, 20) : users;
        log.info("Using {} users (out of {}) for like generation", likeUsers.size(), users.size());
        
        int totalLikes = 0;
        
        for (User user : likeUsers) {
            // 각 사용자가 좋아요할 프로젝트 수 (1-8개, 평균 4개)
            int likesPerUser = random.nextInt(8) + 1;
            List<ProjectRecruitment> likedProjects = new java.util.ArrayList<>();
            
            // 사용자의 기술스택 조회
            List<UserTechStack> userTechStacks = userTechStackRepository.findByUser(user);
            List<String> userTechNames = userTechStacks.stream()
                .map(uts -> uts.getStack().getName().toLowerCase())
                .toList();
            
            if (userTechNames.isEmpty()) {
                continue; // 기술스택이 없는 사용자는 스킵
            }
            
            // 각 프로젝트에 대해 좋아요 확률 계산
            List<ProjectSimilarity> projectSimilarities = new java.util.ArrayList<>();
            
            for (ProjectRecruitment project : projects) {
                // 자신의 프로젝트는 좋아요하지 않음
                if (project.getTeamLeader().getUserId().equals(user.getUserId())) {
                    continue;
                }
                
                // 프로젝트의 기술스택 조회
                List<ProjectTechStack> projectTechStacks = projectTechStackRepository.findByProject(project);
                List<String> projectTechNames = projectTechStacks.stream()
                    .map(pts -> pts.getTechStack().getName().toLowerCase())
                    .toList();
                
                if (projectTechNames.isEmpty()) {
                    continue;
                }
                
                // Jaccard 유사도 계산
                double similarity = calculateJaccardSimilarity(userTechNames, projectTechNames);
                
                // 기본 좋아요 확률 계산
                double baseProbability = calculateLikeProbability(similarity);
                
                // TechPart 매칭 보너스 (사용자와 프로젝트의 기술 파트가 겹치는 경우)
                List<ProjectTechPart> projectTechParts = projectTechPartRepository.findByProject(project);
                
                // 사용자의 프로필에서 기술파트 조회
                com.potential_radar.PR.user.domain.UserProfile userProfile =
                    userProfileRepository.findByUser(user).orElse(null);
                
                boolean techPartMatches = false;
                if (userProfile != null && userProfile.getTechPart() != null) {
                    techPartMatches = projectTechParts.stream()
                        .anyMatch(ptp -> ptp.getTechPart().getTechPartId()
                            .equals(userProfile.getTechPart().getTechPartId()));
                }
                
                if (techPartMatches) {
                    baseProbability *= 1.5; // 50% 보너스
                }
                
                // 프로젝트 생성일 기반 보너스 (최근 프로젝트일수록 좋아요 확률 증가)
                long daysAgo = java.time.Duration.between(project.getCreatedAt(), LocalDateTime.now()).toDays();
                if (daysAgo < 30) {
                    baseProbability *= 1.3; // 30일 내 프로젝트 30% 보너스
                } else if (daysAgo < 90) {
                    baseProbability *= 1.1; // 90일 내 프로젝트 10% 보너스
                }
                
                // 확률 최대값 제한 (80%)
                baseProbability = Math.min(baseProbability, 0.8);
                
                projectSimilarities.add(new ProjectSimilarity(project, similarity, baseProbability));
            }
            
            // 유사도와 확률 기반으로 정렬 (높은 확률 순)
            projectSimilarities.sort((a, b) -> Double.compare(b.probability, a.probability));
            
            // 확률 기반으로 좋아요할 프로젝트 선택
            for (ProjectSimilarity ps : projectSimilarities) {
                if (likedProjects.size() >= likesPerUser) {
                    break;
                }
                
                // 확률 기반 선택
                if (random.nextDouble() < ps.probability) {
                    likedProjects.add(ps.project);
                    
                    try {
                        // 좋아요 생성
                        Like like = Like.builder()
                            .user(user)
                            .targetId(ps.project.getProjectId())
                            .targetType(TargetType.PROJECT)
                            .build();
                        
                        // 좋아요 생성 시점을 프로젝트 생성 이후 ~ 현재 사이로 설정
                        LocalDateTime likeCreatedAt = generateRandomDateBetween(
                            ps.project.getCreatedAt(), 
                            LocalDateTime.now()
                        );
                        like.setCreatedAt(likeCreatedAt);
                        
                        likeRepository.save(like);
                        totalLikes++;
                        
                    } catch (Exception e) {
                        log.warn("⚠️ 좋아요 생성 실패 (사용자 {} → 프로젝트 {}): {}", 
                                user.getUserId(), ps.project.getProjectId(), e.getMessage());
                        // 개별 좋아요 실패는 전체 프로세스를 중단하지 않음
                    }
                }
            }
            
            // 로깅 (10명마다)
            if ((likeUsers.indexOf(user) + 1) % 10 == 0) {
                log.info("Processed {} users, generated {} likes so far...", 
                    likeUsers.indexOf(user) + 1, totalLikes);
            }
        }
        
        log.info("✅ Successfully generated {} smart likes for {} users based on tech stack similarity", 
            totalLikes, likeUsers.size());
        
        // 통계 로깅
        logLikeStatistics(totalLikes, likeUsers.size());
    }
    
    /**
     * Jaccard 유사도 계산
     */
    private double calculateJaccardSimilarity(List<String> userTechs, List<String> projectTechs) {
        if (userTechs.isEmpty() || projectTechs.isEmpty()) {
            return 0.0;
        }
        
        java.util.Set<String> userSet = new java.util.HashSet<>(userTechs);
        java.util.Set<String> projectSet = new java.util.HashSet<>(projectTechs);
        
        java.util.Set<String> intersection = new java.util.HashSet<>(userSet);
        intersection.retainAll(projectSet);
        
        java.util.Set<String> union = new java.util.HashSet<>(userSet);
        union.addAll(projectSet);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * 유사도 기반 좋아요 확률 계산
     */
    private double calculateLikeProbability(double jaccardSimilarity) {
        if (jaccardSimilarity >= 0.5) {
            return 0.7; // 50% 이상 유사 → 70% 확률
        } else if (jaccardSimilarity >= 0.3) {
            return 0.5; // 30-50% 유사 → 50% 확률  
        } else if (jaccardSimilarity >= 0.1) {
            return 0.25; // 10-30% 유사 → 25% 확률
        } else if (jaccardSimilarity > 0) {
            return 0.1; // 조금이라도 유사 → 10% 확률
        } else {
            return 0.02; // 전혀 유사하지 않음 → 2% 확률 (우연한 관심)
        }
    }
    
    /**
     * 두 시점 사이의 랜덤 시간 생성
     */
    private LocalDateTime generateRandomDateBetween(LocalDateTime start, LocalDateTime end) {
        long startSeconds = start.toEpochSecond(java.time.ZoneOffset.UTC);
        long endSeconds = end.toEpochSecond(java.time.ZoneOffset.UTC);
        long randomSeconds = startSeconds + (long) (Math.random() * (endSeconds - startSeconds));
        
        return LocalDateTime.ofEpochSecond(randomSeconds, 0, java.time.ZoneOffset.UTC);
    }
    
    /**
     * 좋아요 통계 로깅
     */
    private void logLikeStatistics(int totalLikes, int totalUsers) {
        double avgLikesPerUser = (double) totalLikes / totalUsers;
        long totalProjects = projectRecruitmentRepository.count();
        double likeRatio = (double) totalLikes / (totalUsers * totalProjects) * 100;
        
        log.info("📊 Like Statistics:");
        log.info("  - Total Likes: {}", totalLikes);
        log.info("  - Average Likes per User: {:.1f}", avgLikesPerUser);
        log.info("  - Overall Like Ratio: {:.2f}% ({}개 중 {}개)", 
            likeRatio, totalUsers * totalProjects, totalLikes);
    }
    
    /**
     * 프로젝트 유사도 및 확률을 저장하는 내부 클래스
     */
    private static class ProjectSimilarity {
        final ProjectRecruitment project;
        final double similarity;
        final double probability;
        
        ProjectSimilarity(ProjectRecruitment project, double similarity, double probability) {
            this.project = project;
            this.similarity = similarity;
            this.probability = probability;
        }
    }
}