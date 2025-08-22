package com.potential_radar.PR.config;

import com.potential_radar.PR.tech.domain.TechPart;
import com.potential_radar.PR.tech.domain.TechStack;
import com.potential_radar.PR.tech.repository.TechPartRepository;
import com.potential_radar.PR.tech.repository.TechStackRepository;
import com.potential_radar.PR.user.domain.*;
import com.potential_radar.PR.user.repository.UserRepository;
import com.potential_radar.PR.user.repository.UserProfileRepository;
import com.potential_radar.PR.project.domain.*;
import com.potential_radar.PR.project.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
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
@Profile("!test") // 테스트 환경에서는 실행하지 않음
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final TechPartRepository techPartRepository;
    private final TechStackRepository techStackRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectTechStackRepository projectTechStackRepository;
    private final ProjectTechPartRepository projectTechPartRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 이미 사용자 테스트 데이터가 있는지 확인
        boolean hasUserData = userRepository.findByEmail("user001@example.com").isPresent();
        // TechStack 데이터가 있는지 확인 (새로 추가된 데이터)
        boolean hasTechStackData = techStackRepository.count() > 20; // 기본 데이터보다 많으면 초기화된 것으로 간주
        // 프로젝트 데이터가 있는지 확인
        boolean hasProjectData = projectRecruitmentRepository.count() > 0;

        log.info("Initializing test data...");
        
        // TechPart 초기화는 TechPartService에서 @PostConstruct로 처리됨
        
        // 1. TechStack 데이터 생성 (항상 실행하여 새 데이터 추가)
        if (!hasTechStackData) {
            initializeTechStacks();
        } else {
            log.info("TechStack data already exists, skipping TechStack initialization");
        }
        
        // 2. 100명의 사용자 데이터 생성
        if (!hasUserData) {
            initializeUsers();
        } else {
            log.info("User data already exists, skipping user initialization");
        }
        
        // 3. 프로젝트 데이터 생성
        if (!hasProjectData) {
            initializeProjects();
        } else {
            log.info("Project data already exists, skipping project initialization");
        }
        
        log.info("Test data initialization completed successfully!");
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
            int userNum = i + 1;
            
            // Provider 결정 (EMAIL 60%, GOOGLE 20%, KAKAO 20%)
            Provider provider;
            if (i % 5 < 3) {
                provider = Provider.EMAIL;
            } else if (i % 5 == 3) {
                provider = Provider.GOOGLE;
            } else {
                provider = Provider.KAKAO;
            }

            // User 생성
            User user = User.builder()
                .email(String.format("user%03d@example.com", userNum))
                .password(provider == Provider.EMAIL ? hashedPassword : null)
                .nickname(nicknames[i])
                .provider(provider)
                .providerUserId(provider != Provider.EMAIL ? 
                    provider.name().toLowerCase() + "_" + (random.nextInt(900000000) + 100000000) : null)
                .profileImage(i % 3 != 0 ? String.format("https://example.com/profile/%03d.jpg", userNum) : null)
                .build();

            user = userRepository.save(user);

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
            "가상현실 기반 원격 교육 시스템"
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
            "VR 기술을 활용하여 몰입감 있는 원격 교육 환경을 제공하는 시스템입니다."
        };

        for (int i = 0; i < 100; i++) {
            // 팀 리더 선택 (랜덤하게 선택)
            User teamLeader = users.get(random.nextInt(users.size()));
            
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


            project = projectRecruitmentRepository.save(project);
            
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
                    .project(project)
                    .techPart(techPart)
                    .recruitCount(random.nextInt(3) + 1) // 각 파트별 1-3명 모집
                    .build();
                
                projectTechPartRepository.save(projectTechPart);
            }
            
            // 프로젝트 기술 스택 연결 (3-8개 랜덤 선택)
            int techStackCount = random.nextInt(6) + 3;
            List<TechStack> selectedTechStacks = new java.util.ArrayList<>();
            
            for (int j = 0; j < techStackCount; j++) {
                TechStack techStack;
                do {
                    techStack = techStacks.get(random.nextInt(techStacks.size()));
                } while (selectedTechStacks.contains(techStack));
                
                selectedTechStacks.add(techStack);
                
                ProjectTechStack projectTechStack = ProjectTechStack.builder()
                    .project(project)
                    .techStack(techStack)
                    .recruitCount(random.nextInt(2) + 1) // 각 기술스택별 1-2명 모집
                    .build();
                
                projectTechStackRepository.save(projectTechStack);
            }
            
            if ((i + 1) % 10 == 0) {
                log.info("Created {} projects...", i + 1);
            }
        }
        
        log.info("Successfully created 100 projects with related tech parts and tech stacks");
    }
}