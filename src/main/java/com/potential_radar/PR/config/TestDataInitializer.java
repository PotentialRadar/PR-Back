package com.potential_radar.PR.config;

import com.potential_radar.PR.tech.domain.TechPart;
import com.potential_radar.PR.tech.domain.TechStack;
import com.potential_radar.PR.tech.repository.TechPartRepository;
import com.potential_radar.PR.tech.repository.TechStackRepository;
import com.potential_radar.PR.user.domain.*;
import com.potential_radar.PR.user.repository.*;
import com.potential_radar.PR.project.domain.*;
import com.potential_radar.PR.project.repository.*;
import com.potential_radar.PR.recommendation.repository.RecommendationHistoryRepository;
import com.potential_radar.PR.invitation.repository.TeamInvitationRepository;
import com.potential_radar.PR.like.domain.Like;
import com.potential_radar.PR.like.domain.TargetType;
import com.potential_radar.PR.like.repository.LikeRepository;
import com.potential_radar.PR.project.repository.TeamMemberReviewRepository;
import com.potential_radar.PR.search.service.DataSyncService;
import com.potential_radar.PR.search.service.SearchCacheService;
import com.potential_radar.PR.search.service.PopularSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class TestDataInitializer implements CommandLineRunner {

    // Repositories & Encoder
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
    private final UserExperienceRepository userExperienceRepository;
    private final UserEducationRepository userEducationRepository;
    private final PortfolioProjectRepository portfolioProjectRepository;
    private final TeamMemberReviewRepository teamMemberReviewRepository;
    private final DataSyncService dataSyncService;
    private final SearchCacheService searchCacheService;
    private final PopularSearchService popularSearchService;
    private final PasswordEncoder passwordEncoder;


    @Override
    @Transactional
    public void run(String... args) throws Exception {
        boolean hasUserData = userRepository.findByEmail("user001@naver.com").isPresent();
        boolean hasTechStackData = techStackRepository.count() > 20;
        boolean hasProjectData = projectRecruitmentRepository.count() > 0;

        log.info("=== Initializing seed data (idempotent for ddl-auto:update) ===");

        initializeTechParts();
        initializeTechStacks();
        initializeUsers(); // 이 부분이 수정되었습니다.

        if (userTechStackRepository.count() == 0) {
            initializeUserTechStacks();
        } else {
            log.info("Skip user tech stacks (already present)");
        }

        if (userExperienceRepository.count() == 0) {
            initializeUserExperiences();
        } else {
            log.info("Skip user experiences (already present)");
        }

        if (userEducationRepository.count() == 0) {
            initializeUserEducations();
        } else {
            log.info("Skip user educations (already present)");
        }

        if (projectRecruitmentRepository.count() == 0) {
            initializeProjects();
        } else {
            log.info("Skip projects (already present)");
        }

        // Elasticsearch & Redis 완전 초기화 후 동기화 (프로젝트 생성 후)
        log.info("Clearing and synchronizing Elasticsearch and Redis after project initialization...");
        try {
            // Redis 모든 데이터 완전 삭제
            searchCacheService.clearAllRedisData();
            log.info("Redis data completely cleared");
            
            // Elasticsearch 기존 데이터 완전 삭제
            dataSyncService.clearAllData();
            log.info("Elasticsearch data completely cleared");
            
            // Elasticsearch 새 데이터로 동기화
            dataSyncService.syncAllData();
            log.info("Elasticsearch sync completed");
            
            // 사용 빈도 기반 인기 기술스택 업데이트
            popularSearchService.updatePopularItems();
            log.info("Popular tech stacks updated based on usage frequency");
        } catch (Exception e) {
            log.warn("Failed to initialize external stores: {}", e.getMessage());
        }

        // ✅ 위 파일 차이 통합: 팀원 리뷰 (프로젝트 생성 후 1회)
        if (teamMemberReviewRepository.count() == 0) {
            initializeTeamMemberReviews();
        } else {
            log.info("Skip team member reviews (already present)");
        }

        log.info("=== Seed data initialization completed ===");
    }

    // --------------------------
    // Seed helpers
    // --------------------------

    private void initializeTechParts() {
        List<String> techPartNames = Arrays.asList(
                "프론트엔드", "백엔드", "풀스택", "모바일", "데브옵스",
                "데이터사이언스", "AI/ML", "게임개발", "보안", "QA/테스터",
                "UI/UX디자인", "PM/기획"
        );

        int created = 0;
        for (String name : techPartNames) {
            if (techPartRepository.findByNameIgnoreCase(name).isEmpty()) {
                techPartRepository.save(TechPart.builder().name(name).build());
                created++;
            }
        }
        log.info("TechPart upsert done. created={}", created);
    }

    private void initializeTechStacks() {
        List<String> techStackNames = Arrays.asList(
                "React","Vue.js","Angular","Next.js","Nuxt.js","Svelte","SvelteKit",
                "JavaScript","TypeScript","HTML5","CSS3","SCSS","Sass","Less",
                "Styled Components","Emotion","Tailwind CSS","Bootstrap","Material-UI",
                "Ant Design","Chakra UI","Webpack","Vite","Parcel","Rollup",
                "ESLint","Prettier","Jest","Cypress","Playwright","React Testing Library","Storybook",
                "Spring Boot","Spring Framework","Spring Security","Spring Data JPA","Spring Cloud",
                "Node.js","Express.js","Nest.js","Fastify","Django","Flask","FastAPI",
                "ASP.NET Core","ASP.NET","Laravel","Symfony","CodeIgniter","Ruby on Rails",
                "Sinatra","Phoenix","Gin","Echo","Fiber","Actix Web","Rocket",
                "Ktor","Quarkus","Micronaut","Helidon",
                "Java","Python","C#","C++","C","Go","Rust","Kotlin","Scala","Clojure",
                "Ruby","PHP","Swift","Objective-C","Dart","R","MATLAB","Perl",
                "Haskell","Elixir","Erlang","F#","VB.NET","COBOL","Fortran",
                "Assembly","Bash","PowerShell",
                "PostgreSQL","MySQL","MariaDB","SQLite","Oracle Database","SQL Server",
                "MongoDB","Redis","Elasticsearch","Cassandra","DynamoDB","CouchDB",
                "Neo4j","InfluxDB","TimescaleDB","Firebase Firestore","Supabase",
                "PlanetScale","Neon","CockroachDB","Amazon RDS","Amazon Aurora",
                "Google Cloud SQL","Azure SQL Database",
                "AWS","AWS EC2","AWS S3","AWS RDS","AWS Lambda","AWS ECS","AWS EKS",
                "AWS CloudFormation","AWS CDK","AWS API Gateway","AWS CloudFront",
                "AWS Route 53","AWS VPC","AWS IAM","AWS SQS","AWS SNS","AWS EventBridge",
                "Amazon ElastiCache","Amazon OpenSearch","Google Cloud Platform",
                "Google Cloud Run","Google Cloud Functions","Google Kubernetes Engine",
                "Google Cloud Storage","Google BigQuery","Firebase","Azure",
                "Azure App Service","Azure Functions","Azure Kubernetes Service",
                "Azure Cosmos DB","Azure Storage","Docker","Kubernetes","Helm",
                "Terraform","Ansible","Chef","Puppet","Vagrant","Jenkins",
                "GitLab CI/CD","GitHub Actions","Azure DevOps","CircleCI","Travis CI",
                "Bitbucket Pipelines","ArgoCD","Flux","Prometheus","Grafana",
                "ELK Stack","Fluentd","Logstash","Kibana","Jaeger","Zipkin",
                "New Relic","Datadog","Sentry","Consul","Vault","Nomad",
                "Istio","Envoy","NGINX","Apache HTTP Server","Traefik","HAProxy","Cloudflare",
                "React Native","Flutter","Ionic","Xamarin","Cordova","SwiftUI",
                "UIKit","Android SDK","Kotlin Multiplatform","Unity","Unreal Engine",
                "TensorFlow","PyTorch","Keras","Scikit-learn","Pandas","NumPy",
                "Matplotlib","Seaborn","Plotly","Jupyter","Anaconda","Apache Spark",
                "Apache Kafka","Apache Airflow","MLflow","Kubeflow","ONNX","OpenCV",
                "Hugging Face","LangChain","OpenAI API","Anthropic Claude","Cohere",
                "JUnit","TestNG","Mockito","Selenium","Cucumber","Postman","Insomnia",
                "k6","Artillery","Apache JMeter","SonarQube","Checkstyle","SpotBugs","PMD",
                "Git","GitHub","GitLab","Bitbucket","Azure Repos","SVN","Mercurial",
                "Jira","Confluence","Slack","Microsoft Teams","Discord","Notion",
                "Trello","Asana","Linear","Monday.com",
                "Figma","Adobe XD","Sketch","InVision","Zeplin","Adobe Photoshop",
                "Adobe Illustrator","Canva","Framer","Principle",
                "Visual Studio Code","IntelliJ IDEA","Eclipse","NetBeans","Xcode",
                "Android Studio","PyCharm","WebStorm","PhpStorm","RubyMine",
                "GoLand","CLion","DataGrip","Rider","Visual Studio","Vim",
                "Emacs","Sublime Text","Atom","Brackets",
                "GraphQL","REST API","gRPC","WebSocket","Socket.io","OAuth 2.0",
                "JWT","OpenAPI","Swagger","Progressive Web App","Service Worker",
                "WebAssembly","WebRTC",
                "Unity 3D","Unreal Engine 4","Unreal Engine 5","Godot","GameMaker Studio",
                "Construct 3","Phaser","Three.js","Babylon.js","OpenGL","DirectX","Vulkan",
                "Ethereum","Solidity","Web3.js","Ethers.js","Hardhat","Truffle",
                "Polygon","Binance Smart Chain","Solana","Cardano","Polkadot",
                "Hyperledger","IPFS",
                "Arduino","Raspberry Pi","ESP32","ESP8266","STM32","FreeRTOS",
                "Zephyr","Mbed","PlatformIO","MQTT","CoAP","LoRaWAN",
                "OWASP","Burp Suite","Metasploit","Nmap","Wireshark","Kali Linux",
                "Penetration Testing","Ethical Hacking","SSL/TLS","PKI","OAuth",
                "SAML","LDAP","Active Directory",
                "VMware","VirtualBox","QEMU","Proxmox","Hyper-V","Docker Compose",
                "Docker Swarm","Podman","LXC","OpenShift","Rancher",
                "Nagios","Zabbix","Cacti","PRTG","SolarWinds","Splunk",
                "Elastic APM","Application Insights","CloudWatch","Stackdriver",
                "TCP/IP","HTTP/HTTPS","DNS","BGP","OSPF","VLAN","VPN","SDN",
                "OpenFlow","Cisco","Juniper","Palo Alto","Fortinet",
                "Tableau","Power BI","QlikView","Looker","Metabase","Apache Superset",
                "D3.js","Chart.js","Highcharts","Google Analytics","Adobe Analytics",
                "WordPress","Drupal","Joomla","Magento","Shopify","WooCommerce",
                "PrestaShop","OpenCart","BigCommerce","Strapi","Contentful",
                "Sanity","Ghost","Headless CMS",
                "RabbitMQ","Apache ActiveMQ","Amazon SQS","Google Pub/Sub",
                "Azure Service Bus","NATS","ZeroMQ","Pulsar"
        );

        int created = 0;
        for (String name : techStackNames) {
            if (techStackRepository.findByNameIgnoreCase(name).isEmpty()) {
                techStackRepository.save(TechStack.builder().name(name).build());
                created++;
            }
        }
        log.info("TechStack upsert done. created={}", created);
    }

    private void initializeUsers() {
        List<TechPart> techParts = techPartRepository.findAll();
        if (techParts.isEmpty()) {
            log.error("TechPart is empty. initializeTechParts()가 먼저 호출되어야 합니다.");
            return;
        }

        Random random = new Random();
        String hashedPassword = passwordEncoder.encode("1234");

        String[] nicknames = {
                "코딩마스터001","개발자김철수","프론트엔드박영희","백엔드이민수","풀스택홍길동",
                "ReactDeveloper","SpringMaster","NodeJSExpert","VueDeveloper","PythonGuru",
                "JavaScriptNinja","SQLMaster","CSSWizard","HTMLCoder","TypeScriptDev",
                "AndroidDev","iOSExpert","FlutterCoder","ReactNativeDev","SwiftProgrammer",
                "KotlinDev","DevOpsEngineer","DockerMaster","KubernetesExpert","AWSSpecialist",
                "DataScientist","MLEngineer","AIResearcher","BigDataAnalyst","TensorFlowDev",
                "GameDeveloper","UnityExpert","UnrealCoder","GameDesigner","GraphicsProgrammer",
                "SecurityExpert","PenetrationTester","CybersecurityAnalyst","SecurityArchitect","CryptographyExpert",
                "QAEngineer","TestAutomation","PerformanceTester","ManualTester","QualityAssurance",
                "UIDesigner","UXResearcher","ProductDesigner","InteractionDesigner","VisualDesigner",
                "ProductManager","ProjectManager","TechLead","ScrumMaster","BusinessAnalyst",
                "SystemArchitect","DatabaseAdmin","NetworkEngineer","CloudArchitect","InfraEngineer",
                "JavaDeveloper","CSharpDev","GoDeveloper","RustProgrammer","PhpDeveloper",
                "RubyDeveloper","ScalaDeveloper","ElixirCoder","HaskellDev","ClojureCoder",
                "BlockchainDev","SmartContractDev","Web3Developer","DAppDeveloper","NFTCreator",
                "IoTDeveloper","EmbeddedEngineer","FirmwareDev","HardwareEngineer","RoboticsEngineer",
                "ARDeveloper","VRDeveloper","XREngineer","MetaverseDev","3DGraphicsDev",
                "QuantumComputing","CloudNativeDev","MicroservicesArch","ServerlessExpert","EdgeComputingDev",
                "TechEvangelist","DeveloperAdvocate","TechnicalWriter","CodeReviewer","OpenSourceMaintainer",
                "CommunityManager","TechRecruiter","StartupFounder","TechConsultant","DigitalNomad"
        };

        ExperienceRange[] experienceRanges = ExperienceRange.values();

        for (int i = 0; i < 100; i++) {
            int userNum = i + 1;
            String email = String.format("user%03d@naver.com", userNum);
            Provider provider = (i % 5 < 3) ? Provider.EMAIL : (i % 5 == 3 ? Provider.GOOGLE : Provider.KAKAO);

            int finalI = i;
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                // 1부터 12 사이의 랜덤 이미지 번호 생성
                int randomImageNum = random.nextInt(12) + 1;
                String profileImageUrl = String.format("https://example.com/profile/%03d.jpg", randomImageNum);

                User u = User.builder()
                        .email(email)
                        .password(provider == Provider.EMAIL ? hashedPassword : null)
                        .nickname(nicknames[finalI])
                        .provider(provider)
                        .providerUserId(provider != Provider.EMAIL
                                ? provider.name().toLowerCase() + "_" + (random.nextInt(900000000) + 100000000)
                                : null)
                        .profileImage(profileImageUrl) // 랜덤 이미지 URL로 설정
                        .build();
                return userRepository.save(u);
            });

            if (userProfileRepository.findById(user.getUserId()).isEmpty()) {
                TechPart techPart = techParts.get(i % techParts.size());

                UserProfile profile = UserProfile.builder()
                        .user(user)
                        .techPart(techPart)
                        .bio(i % 3 == 0 ? null :
                                (i % 5 == 0
                                        ? "안녕하세요! " + nicknames[i] + "입니다. 다양한 프로젝트 경험을 통해 성장하고 있습니다."
                                        : "열정적인 개발자 " + nicknames[i] + "입니다. 새로운 기술 학습과 협업을 좋아합니다. 함께 멋진 프로젝트를 만들어봅시다!"))
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
                        .reputationScore(BigDecimal.valueOf(Math.round((new Random().nextDouble() * 4.5 + 0.5) * 100.0) / 100.0))
                        .reviewCount(new Random().nextInt(51))
                        .experienceRange(experienceRanges[i % experienceRanges.length])
                        .build();

                userProfileRepository.save(profile);
            }
        }
        log.info("Users & Profiles upserted to 100");
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
        log.info("Creating projects ...");

        List<User> users = userRepository.findAll();
        List<TechPart> techParts = techPartRepository.findAll();
        List<TechStack> techStacks = techStackRepository.findAll();
        Random random = new Random();

        if (users.isEmpty() || techParts.isEmpty() || techStacks.isEmpty()) {
            log.error("필수 데이터(사용자, 기술 파트, 기술 스택)가 존재하지 않아 프로젝트를 생성할 수 없습니다.");
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
                "Spring Boot 마이크로서비스 아키텍처를 설계합니다.",
                "Flutter를 사용하여 iOS와 Android에서 동시에 작동하는 모바일 앱을 개발합니다.",
                "Vue.js와 최신 프론트엔드 기술을 활용한 전자상거래 웹사이트를 구현합니다.",
                "Django REST Framework로 확장성 있는 백엔드 API 서버를 구축합니다.",
                "Angular를 활용한 반응형 관리자 대시보드 시스템을 개발합니다.",
                "Node.js와 Socket.io를 사용한 실시간 멀티채널 채팅 서비스를 구현합니다.",
                "Python과 Pandas, NumPy를 활용한 빅데이터 분석 플랫폼을 구축합니다.",
                "Java와 Spring 생태계를 활용한 대규모 엔터프라이즈 솔루션을 개발합니다.",
                "TypeScript의 타입 안정성을 활용한 프로젝트 관리 도구를 구현합니다.",
                "Go 언어 기반 고성능 API 서버를 개발합니다.",
                "Rust 기반 시스템 프로그래밍 도구를 구축합니다.",
                "Unity 3D 인디 게임 개발",
                "React Native 모바일 커머스 앱",
                "Kotlin Android 네이티브 앱",
                "Swift iOS 소셜 미디어 앱",
                "Docker 컨테이너 기반 DevOps 플랫폼",
                "Kubernetes 클러스터 관리 시스템",
                "PostgreSQL 기반 데이터베이스 설계",
                "MongoDB NoSQL 문서 관리 시스템"
        };

        for (int i = 0; i < 50; i++) {
            User teamLeader = users.get(random.nextInt(users.size()));

            LocalDate today = LocalDate.now();
            LocalDate recruitDeadline = today.plusDays(random.nextInt(30) + 10);
            LocalDate startDate = recruitDeadline.plusDays(random.nextInt(14) + 1);
            LocalDate endDate = startDate.plusDays(random.nextInt(120) + 30);

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

            ProjectRecruitment saved = projectRecruitmentRepository.save(project);

            // Tech Parts (1~3)
            int techPartCount = random.nextInt(3) + 1;
            List<TechPart> selectedParts = new ArrayList<>();
            for (int j = 0; j < techPartCount; j++) {
                TechPart tp;
                do {
                    tp = techParts.get(random.nextInt(techParts.size()));
                } while (selectedParts.contains(tp));
                selectedParts.add(tp);

                projectTechPartRepository.save(ProjectTechPart.builder()
                        .project(saved)
                        .techPart(tp)
                        .recruitCount(random.nextInt(3) + 1)
                        .build());
            }

            // Tech Stacks
            for (String techName : getProjectTechStacks(i)) {
                techStackRepository.findByNameIgnoreCase(techName).ifPresent(ts -> {
                    projectTechStackRepository.save(ProjectTechStack.builder()
                            .project(saved)
                            .techStack(ts)
                            .recruitCount(new Random().nextInt(2) + 1)
                            .build());
                });
            }

            // Members (leader + 2~5)
            projectMemberRepository.save(ProjectMember.builder()
                    .project(saved)
                    .user(teamLeader)
                    .role(ProjectMember.Role.LEADER)
                    .techPart(selectedParts.get(0).getName())
                    .build());

            int additional = random.nextInt(4) + 2;
            Set<Long> usedUserIds = new HashSet<>();
            usedUserIds.add(teamLeader.getUserId());

            for (int j = 0; j < additional; j++) {
                User member;
                do {
                    member = users.get(random.nextInt(users.size()));
                } while (!usedUserIds.add(member.getUserId()));

                String memberPart = selectedParts.get(random.nextInt(selectedParts.size())).getName();

                projectMemberRepository.save(ProjectMember.builder()
                        .project(saved)
                        .user(member)
                        .role(ProjectMember.Role.MEMBER)
                        .techPart(memberPart)
                        .build());
            }
        }

        log.info("Projects created: {}", 50);
    }

    private List<String> getProjectTechStacks(int projectIndex) {
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

    private void initializeUserTechStacks() {
        log.info("Assigning user tech stacks ...");
        List<User> users = userRepository.findAll();
        List<TechStack> techStacks = techStackRepository.findAll();
        Random random = new Random();

        for (User user : users) {
            int count = random.nextInt(5) + 3;
            Set<Long> used = new HashSet<>();
            for (int i = 0; i < count; i++) {
                TechStack ts;
                do {
                    ts = techStacks.get(random.nextInt(techStacks.size()));
                } while (!used.add(ts.getTechStackId()));

                userTechStackRepository.save(UserTechStack.builder()
                        .user(user)
                        .stack(ts)
                        .skillLevel(random.nextInt(5) + 1)
                        .build());
            }
        }
        log.info("User tech stacks assigned to {} users", users.size());
    }

    private void initializeUserExperiences() {
        log.info("Creating user experiences ...");
        List<User> users = userRepository.findAll();
        Random random = new Random();

        String[] companyNames = {
                "네이버","카카오","라인","쿠팡","배달의민족","토스","당근마켓","야놀자",
                "NHN","넥슨","엔씨소프트","넷마블","크래프톤","스마일게이트","위메프","11번가",
                "SK텔레콤","KT","LG유플러스","삼성SDS","LG CNS","포스코DX","현대오토에버",
                "우아한형제들","마켓컬리","직방","집닥","29CM","무신사","브랜디","에이블리",
                "Google","Microsoft","Amazon","Meta","Apple","Netflix","Uber","Airbnb"
        };

        String[] departments = {
                "개발팀","프론트엔드팀","백엔드팀","모바일팀","데브옵스팀","인프라팀",
                "데이터팀","AI팀","ML팀","보안팀","QA팀","제품팀","기획팀","디자인팀"
        };

        String[] summaries = {
                "웹 애플리케이션 개발 및 유지보수 담당",
                "RESTful API 설계 및 구현",
                "프론트엔드 컴포넌트 개발 및 최적화",
                "모바일 앱 개발 및 성능 개선",
                "데이터베이스 설계 및 쿼리 최적화",
                "CI/CD 파이프라인 구축 및 관리",
                "마이크로서비스 아키텍처 설계",
                "사용자 경험 개선을 위한 UI/UX 개발",
                "대용량 트래픽 처리 시스템 개발",
                "클라우드 인프라 구축 및 운영"
        };

        int total = 0;
        for (User user : users) {
            int experienceCount = (random.nextInt(10) < 7) ? (random.nextInt(2) + 1) : (random.nextInt(2) == 0 ? 0 : 3);
            LocalDate current = LocalDate.now();

            for (int i = 0; i < experienceCount; i++) {
                String company = companyNames[random.nextInt(companyNames.length)];
                String dept = departments[random.nextInt(departments.length)];
                String summary = summaries[random.nextInt(summaries.length)];

                LocalDate endDate;
                LocalDate startDate;
                boolean isCurrent = false;

                if (i == 0 && random.nextInt(10) < 4) {
                    isCurrent = true;
                    endDate = null;
                    startDate = current.minusMonths(random.nextInt(36) + 6);
                } else {
                    int monthsAgo = 6 + i * 12 + random.nextInt(24);
                    endDate = current.minusMonths(monthsAgo);
                    int durationMonths = random.nextInt(24) + 6;
                    startDate = endDate.minusMonths(durationMonths);
                }

                userExperienceRepository.save(UserExperience.builder()
                        .user(user)
                        .companyName(company)
                        .department(dept)
                        .startDate(startDate)
                        .endDate(endDate)
                        .isCurrent(isCurrent)
                        .summary(summary)
                        .build());
                total++;
            }
        }
        log.info("User experiences created: {}", total);
    }

    private void initializeUserEducations() {
        log.info("Creating user educations ...");
        List<User> users = userRepository.findAll();
        Random random = new Random();

        String[] universities = {
                "서울대학교","연세대학교","고려대학교","성균관대학교","한양대학교",
                "중앙대학교","경희대학교","서강대학교","이화여자대학교","건국대학교",
                "동국대학교","홍익대학교","숭실대학교","국민대학교","KAIST",
                "포항공과대학교","부산대학교","경북대학교","전남대학교","충남대학교"
        };

        String[] programs = {
                "컴퓨터공학과","소프트웨어학과","정보통신공학과","전자공학과","컴퓨터과학과",
                "데이터사이언스학과","인공지능학과","게임학과","정보보호학과","산업공학과",
                "시각디자인학과","경영학과","경영정보학과","멀티미디어학과","수학과"
        };

        int total = 0;
        for (User user : users) {
            int count = random.nextInt(10) < 8 ? 1 : 2;
            LocalDate current = LocalDate.now();

            for (int i = 0; i < count; i++) {
                String institution = universities[random.nextInt(universities.length)];
                String program = programs[random.nextInt(programs.length)];
                String degree = (i == 0) ? "학사" : "석사";
                String programWithDegree = program + " " + degree;

                int yearsAgo = 2 + random.nextInt(7);
                LocalDate endDate = current.minusYears(yearsAgo);
                LocalDate startDate = endDate.minusYears(degree.equals("학사") ? 4 : 2);

                userEducationRepository.save(UserEducation.builder()
                        .user(user)
                        .institution(institution)
                        .program(programWithDegree)
                        .startDate(startDate)
                        .endDate(endDate)
                        .isCurrent(false)
                        .build());
                total++;
            }
        }
        log.info("User educations created: {}", total);
    }

    private void initializeTeamMemberReviews() {
        log.info("Creating team member reviews ...");

        List<ProjectRecruitment> projects = projectRecruitmentRepository.findAll();
        Random random = new Random();

        String[] positive = {
                "정말 훌륭한 팀원이었습니다! 프로젝트에 큰 도움이 되었어요.",
                "커뮤니케이션이 원활하고 책임감이 강한 개발자입니다.",
                "기술적 역량이 뛰어나고 팀워크가 좋습니다.",
                "문제 해결 능력이 뛰어나고 적극적으로 참여해주셨습니다.",
                "코드 품질이 우수하고 일정 관리를 잘 해주셨어요.",
                "창의적인 아이디어를 많이 제안해주시고 실행력이 좋습니다.",
                "다른 팀원들과의 협업이 매우 원활했습니다.",
                "기한을 잘 지키시고 꼼꼼하게 작업해주셨습니다."
        };

        String[] neutral = {
                "전반적으로 무난한 팀원이었습니다.",
                "맡은 역할을 잘 수행해주셨습니다.",
                "프로젝트에 성실하게 참여해주셨어요.",
                "기본기가 탄탄한 개발자입니다.",
                "주어진 업무를 차근차근 완수해주셨습니다."
        };

        int total = 0;
        for (ProjectRecruitment project : projects) {
            List<ProjectMember> members = projectMemberRepository.findAllByProject_ProjectId(project.getProjectId());
            if (members.size() < 2) continue;

            for (ProjectMember reviewer : members) {
                for (ProjectMember reviewee : members) {
                    if (reviewer.getUser().getUserId().equals(reviewee.getUser().getUserId())) continue;
                    if (!random.nextBoolean()) continue;

                    int rating = random.nextInt(3) + 3;
                    String comment = (rating >= 4)
                            ? positive[random.nextInt(positive.length)]
                            : neutral[random.nextInt(neutral.length)];

                    teamMemberReviewRepository.save(TeamMemberReview.builder()
                            .project(project)
                            .reviewer(reviewer.getUser())
                            .reviewee(reviewee.getUser())
                            .rating(rating)
                            .comment(comment)
                            .build());
                    total++;
                }
            }
        }
        log.info("Team member reviews created: {}", total);
    }
}