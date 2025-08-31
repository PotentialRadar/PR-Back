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
@Profile("!test")
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

    private final UserExperienceRepository userExperienceRepository;
    private final UserEducationRepository userEducationRepository;
    private final PortfolioProjectRepository portfolioProjectRepository; // 현재 사용 X, 차이만 반영
    private final TeamMemberReviewRepository teamMemberReviewRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        boolean hasUserData = userRepository.findByEmail("user001@naver.com").isPresent();
        boolean hasTechStackData = techStackRepository.count() > 20;
        boolean hasProjectData = projectRecruitmentRepository.count() > 0;

        log.info("=== Initializing seed data (idempotent for ddl-auto:update) ===");
        log.info("Data check results - Users: {}, TechStacks: {}, Projects: {}", hasUserData, hasTechStackData, hasProjectData);

        // recommendation_history 구조 업데이트 (밑 파일 로직 유지)
        updateRecommendationHistoryTable();

        // 1) TechPart
        initializeTechParts();

        // 2) TechStack (밑 파일 정책 유지: 충분하면 skip)
        if (!hasTechStackData) {
            initializeTechStacks();
        } else {
            log.info("TechStack data already exists, skipping TechStack initialization");
        }

        // 3) Users (밑 파일 정책 유지)
        if (!hasUserData) {
            initializeUsers();
        } else {
            log.info("User data already exists, skipping user initialization");
        }

        // 4) UserTechStacks (밑 파일 정책 유지)
        boolean hasUserTechStackData = userTechStackRepository.count() > 0;
        if (!hasUserTechStackData) {
            initializeUserTechStacks();
        } else {
            log.info("User tech stack data already exists, skipping user tech stack initialization");
        }

        // ✅ 위 파일 차이 통합: User Experiences/Educations (한 번만)
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

        // 5) 프로젝트 데이터 (밑 파일 정책: 기존 있으면 깨끗이 지우고 재생성)
        if (hasProjectData) {
            log.info("Clearing existing project data for tech stack improvement...");
            try {
                jdbcTemplate.execute("DELETE FROM recommendation_feedback");
                log.info("Deleted recommendation feedbacks");

                likeRepository.deleteAll();
                log.info("Deleted likes");

                recommendationHistoryRepository.deleteAll();
                log.info("Deleted recommendation histories");

                teamInvitationRepository.deleteAll();
                log.info("Deleted team invitations");

                projectMemberRepository.deleteAll();
                log.info("Deleted project members");

                projectApplicationRepository.deleteAll();
                log.info("Deleted project applications");

                projectTechStackRepository.deleteAll();
                projectTechPartRepository.deleteAll();
                log.info("Deleted project relations");

                projectRecruitmentRepository.deleteAll();
                log.info("Deleted projects");
            } catch (Exception e) {
                log.error("Failed to delete existing project data: {}", e.getMessage());
            }
        }
        initializeProjects();

        // ✅ 위 파일 차이 통합: 팀원 리뷰 (프로젝트 생성 후 1회)
        if (teamMemberReviewRepository.count() == 0) {
            initializeTeamMemberReviews();
        } else {
            log.info("Skip team member reviews (already present)");
        }

        // 6) 스마트 Like 생성 (밑 파일 정책 유지)
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
                TechPart techPart = TechPart.builder().name(name).build();
                techPartRepository.save(techPart);
            }
        }
    }

    private void initializeTechStacks() {
        List<String> techStackNames = Arrays.asList(
                // (밑 파일과 동일: 생략 없이 유지)
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

        log.info("Initializing TechStack data...");
        List<TechStack> newTechStacks = new java.util.ArrayList<>();
        for (String name : techStackNames) {
            if (techStackRepository.findByNameIgnoreCase(name).isEmpty()) {
                newTechStacks.add(TechStack.builder().name(name).build());
            }
        }
        if (!newTechStacks.isEmpty()) techStackRepository.saveAll(newTechStacks);
        log.info("Created {} new TechStack entries", newTechStacks.size());
    }

    private void initializeUsers() {
        List<TechPart> techParts = techPartRepository.findAll();
        Random random = new Random();
        String hashedPassword = passwordEncoder.encode("1234"); // 밑 파일 유지

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

        List<User> newUsers = new java.util.ArrayList<>();
        List<UserProfile> userProfiles = new java.util.ArrayList<>();

        for (int i = 0; i < 100; i++) {
            int userNum = i + 1;
            String email = String.format("user%03d@naver.com", userNum);
            Provider provider = (i % 5 < 3) ? Provider.EMAIL : (i % 5 == 3 ? Provider.GOOGLE : Provider.KAKAO);

            if (userRepository.findByEmail(email).isPresent()) continue;

            String baseNickname = nicknames[i];
            String uniqueNickname = baseNickname;
            int suffix = 1;
            while (userRepository.findByNickname(uniqueNickname).isPresent()) {
                uniqueNickname = baseNickname + "_" + suffix++;
            }

            User user = User.builder()
                    .email(email)
                    .password(hashedPassword)
                    .nickname(uniqueNickname)
                    .provider(provider)
                    .providerUserId(provider != Provider.EMAIL
                            ? provider.name().toLowerCase() + "_" + (random.nextInt(900000000) + 100000000)
                            : null)
                    .profileImage(null)
                    .build();
            newUsers.add(user);
        }

        if (!newUsers.isEmpty()) {
            List<User> saved = userRepository.saveAll(newUsers);
            for (int i = 0; i < saved.size(); i++) {
                User user = saved.get(i);
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
                                String.format("010-%04d-%04d", ((i + 1) * 37) % 10000, ((i + 1) * 73) % 10000))
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

                userProfiles.add(profile);
            }
            userProfileRepository.saveAll(userProfiles);
            log.info("Created {} users with profiles", saved.size());
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
                "AI 기반 개인 맞춤형 학습 플랫폼 개발","실시간 협업 화이트보드 웹 애플리케이션",
                "블록체인 기반 탈중앙화 소셜 미디어","IoT 스마트 홈 자동화 시스템",
                "머신러닝을 활용한 주식 투자 분석 도구","모바일 AR 쇼핑 경험 애플리케이션",
                "클라우드 기반 팀 프로젝트 관리 도구","실시간 언어 번역 화상 회의 서비스",
                "게임화된 온라인 코딩 교육 플랫폼","환경 데이터 모니터링 및 분석 시스템",
                "NFT 기반 디지털 아트 마켓플레이스","음성 인식 기반 AI 비서 앱",
                "실시간 건강 모니터링 웨어러블 연동 앱","지속가능한 에너지 관리 스마트 그리드",
                "VR 가상 여행 체험 플랫폼","자율주행차 시뮬레이션 및 테스트 환경",
                "개인화된 뉴스 큐레이션 AI 서비스","온라인 멘토링 매칭 플랫폼",
                "실시간 재난 알림 및 대피 안내 시스템","크라우드펀딩 기반 스타트업 지원 플랫폼",
                "AI 기반 개인 영양사 및 식단 관리 앱","실시간 주차 공간 찾기 및 예약 서비스",
                "블록체인 기반 투명한 기부 추적 시스템","음악 AI 작곡 및 편집 도구",
                "스마트 시티 교통 최적화 솔루션","온라인 의료 상담 및 처방 플랫폼",
                "게임 스트리밍 및 커뮤니티 플랫폼","AI 기반 법률 자문 챗봇 서비스",
                "실시간 농작물 모니터링 스마트팜","가상현실 기반 원격 교육 시스템",
                "React 기반 소셜 네트워킹 플랫폼","Spring Boot 마이크로서비스 아키텍처",
                "Flutter 크로스플랫폼 모바일 앱","Vue.js 기반 전자상거래 사이트",
                "Django REST API 백엔드 서버","Angular 기반 대시보드 시스템",
                "Node.js 실시간 채팅 애플리케이션","Python 데이터 분석 플랫폼",
                "Java 기반 엔터프라이즈 솔루션","TypeScript 기반 프로젝트 관리 도구",
                "Go 언어 기반 고성능 API 서버","Rust 기반 시스템 프로그래밍 도구",
                "Unity 3D 인디 게임 개발","React Native 모바일 커머스 앱",
                "Kotlin Android 네이티브 앱","Swift iOS 소셜 미디어 앱",
                "Docker 컨테이너 기반 DevOps 플랫폼","Kubernetes 클러스터 관리 시스템",
                "PostgreSQL 기반 데이터베이스 설계","MongoDB NoSQL 문서 관리 시스템"
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
            // 밑 파일 로직: user001~005가 각각 10개씩 리더
            int teamLeaderIndex = i / 10;
            User teamLeader = users.get(teamLeaderIndex);

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

            // 밑 파일: createdAt 무작위 과거로
            project.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(365)));

            final ProjectRecruitment savedProject = projectRecruitmentRepository.save(project);

            // TechPart (1~3)
            int techPartCount = random.nextInt(3) + 1;
            List<TechPart> selectedTechParts = new java.util.ArrayList<>();
            for (int j = 0; j < techPartCount; j++) {
                TechPart techPart;
                do {
                    techPart = techParts.get(random.nextInt(techParts.size()));
                } while (selectedTechParts.contains(techPart));
                selectedTechParts.add(techPart);

                projectTechPartRepository.save(ProjectTechPart.builder()
                        .project(savedProject)
                        .techPart(techPart)
                        .recruitCount(random.nextInt(3) + 1)
                        .build());
            }

            // TechStacks (밑 파일의 getProjectTechStacks 사용)
            List<String> projectTechStackNames = getProjectTechStacks(i, projectTitles[i % projectTitles.length]);
            for (String name : projectTechStackNames) {
                techStackRepository.findByNameIgnoreCase(name).ifPresent(techStack ->
                        projectTechStackRepository.save(
                                ProjectTechStack.builder()
                                        .project(savedProject)
                                        .techStack(techStack)
                                        .recruitCount(random.nextInt(2) + 1)
                                        .build()
                        )
                );
            }

            if ((i + 1) % 10 == 0) log.info("Created {} projects...", i + 1);
        }

        log.info("Successfully created 50 projects with related tech parts and tech stacks");
    }

    // 밑 파일 버전 유지
    private List<String> getProjectTechStacks(int projectIndex, String projectTitle) {
        return switch (projectIndex % 50) {
            case 0 -> Arrays.asList("React","TypeScript","Node.js","Python","PostgreSQL","Docker");
            case 1 -> Arrays.asList("React","JavaScript","Node.js","Express.js","Socket.io","MongoDB");
            case 2 -> Arrays.asList("Vue.js","JavaScript","Python","Django","PostgreSQL","Redis");
            case 3 -> Arrays.asList("Java","Spring Boot","Python","PostgreSQL","Docker","AWS");
            case 4 -> Arrays.asList("Python","Django","React","TypeScript","PostgreSQL","TensorFlow");
            case 5 -> Arrays.asList("React Native","TypeScript","Node.js","MongoDB","AWS");
            case 6 -> Arrays.asList("Angular","TypeScript","Java","Spring Boot","PostgreSQL","Docker");
            case 7 -> Arrays.asList("React","TypeScript","Python","FastAPI","PostgreSQL","Docker");
            case 8 -> Arrays.asList("Flutter","Dart","Firebase","Node.js","MongoDB");
            case 9 -> Arrays.asList("Python","NumPy","Pandas","Scikit-learn","PostgreSQL","Docker");
            case 10 -> Arrays.asList("React","TypeScript","Solidity","Web3.js","MongoDB");
            case 11 -> Arrays.asList("Python","TensorFlow","React","Node.js","PostgreSQL");
            case 12 -> Arrays.asList("React Native","TypeScript","Firebase","AWS");
            case 13 -> Arrays.asList("Java","Spring Boot","React","PostgreSQL","Docker","Kubernetes");
            case 14 -> Arrays.asList("Unity","C#","Blender","Firebase");
            case 15 -> Arrays.asList("Python","Selenium","React","Node.js","PostgreSQL");
            case 16 -> Arrays.asList("React","TypeScript","Python","TensorFlow","PostgreSQL");
            case 17 -> Arrays.asList("Angular","TypeScript","Java","Spring Boot","PostgreSQL");
            case 18 -> Arrays.asList("Python","Django","React","PostgreSQL","AWS");
            case 19 -> Arrays.asList("React","JavaScript","Node.js","MongoDB","AWS");
            case 20 -> Arrays.asList("Python","TensorFlow","React","Node.js","MongoDB");
            case 21 -> Arrays.asList("React","TypeScript","Node.js","PostgreSQL","Redis");
            case 22 -> Arrays.asList("Solidity","Web3.js","React","Node.js","MongoDB");
            case 23 -> Arrays.asList("Python","TensorFlow","React","Node.js","PostgreSQL");
            case 24 -> Arrays.asList("Java","Spring Boot","React","PostgreSQL","AWS");
            case 25 -> Arrays.asList("React","TypeScript","Node.js","PostgreSQL","Docker");
            case 26 -> Arrays.asList("Unity","C#","React","Node.js","MongoDB");
            case 27 -> Arrays.asList("Python","TensorFlow","React","PostgreSQL","Docker");
            case 28 -> Arrays.asList("Java","Spring Boot","Python","PostgreSQL","Docker");
            case 29 -> Arrays.asList("Unity","C#","Blender","Node.js","MongoDB");
            case 30 -> Arrays.asList("React","TypeScript","Node.js","MongoDB","Docker");
            case 31 -> Arrays.asList("Java","Spring Boot","PostgreSQL","Docker","Kubernetes");
            case 32 -> Arrays.asList("Flutter","Dart","Firebase","MongoDB");
            case 33 -> Arrays.asList("Vue.js","JavaScript","Node.js","PostgreSQL","Docker");
            case 34 -> Arrays.asList("Python","Django","PostgreSQL","Redis","Docker");
            case 35 -> Arrays.asList("Angular","TypeScript","Java","PostgreSQL","Docker");
            case 36 -> Arrays.asList("Node.js","JavaScript","Socket.io","MongoDB","Redis");
            case 37 -> Arrays.asList("Python","Pandas","NumPy","PostgreSQL","Docker");
            case 38 -> Arrays.asList("Java","Spring Boot","PostgreSQL","Docker","AWS");
            case 39 -> Arrays.asList("TypeScript","React","Node.js","PostgreSQL","Docker");
            case 40 -> Arrays.asList("Go","PostgreSQL","Redis","Docker","Kubernetes");
            case 41 -> Arrays.asList("Rust","PostgreSQL","Docker","Linux");
            case 42 -> Arrays.asList("Unity","C#","Blender","Firebase");
            case 43 -> Arrays.asList("React Native","TypeScript","Firebase","MongoDB");
            case 44 -> Arrays.asList("Kotlin","Android","Firebase","SQLite");
            case 45 -> Arrays.asList("Swift","iOS","Firebase","CoreData");
            case 46 -> Arrays.asList("Docker","Kubernetes","Jenkins","AWS","Linux");
            case 47 -> Arrays.asList("Kubernetes","Docker","Helm","AWS","Linux");
            case 48 -> Arrays.asList("PostgreSQL","Docker","pgAdmin","AWS");
            case 49 -> Arrays.asList("MongoDB","Node.js","Express.js","Docker");
            default -> Arrays.asList("JavaScript","Node.js","MongoDB");
        };
    }

    /**
     * 사용자 기술스택 초기화 (밑 파일 로직 유지)
     */
    private void initializeUserTechStacks() {
        log.info("Initializing user tech stack data...");
        List<User> users = userRepository.findAll();
        List<TechStack> techStacks = techStackRepository.findAll();
        Random random = new Random();

        if (users.isEmpty() || techStacks.isEmpty()) {
            log.warn("Required data not found. Users: {}, TechStacks: {}", users.size(), techStacks.size());
            return;
        }

        String[] commonTechNames = {
                "React","TypeScript","JavaScript","Node.js","Python","Java","Spring Boot",
                "PostgreSQL","MongoDB","Docker","AWS","Vue.js","Angular","Django",
                "FastAPI","Express.js","MySQL","Redis","Flutter","React Native",
                "Kubernetes","TensorFlow","Unity 3D","HTML","CSS","Git","Linux"
        };

        List<TechStack> commonTechStacks = new java.util.ArrayList<>();
        List<TechStack> otherTechStacks = new java.util.ArrayList<>();
        for (TechStack s : techStacks) {
            boolean isCommon = false;
            for (String cn : commonTechNames) {
                if (s.getName().equalsIgnoreCase(cn)) { isCommon = true; break; }
            }
            if (isCommon) commonTechStacks.add(s); else otherTechStacks.add(s);
        }

        List<UserTechStack> all = new java.util.ArrayList<>();
        for (User user : users) {
            int techStackCount = new Random().nextInt(4) + 3; // 3-6
            List<TechStack> picked = new java.util.ArrayList<>();
            for (int i = 0; i < techStackCount; i++) {
                TechStack pick;
                int attempts = 0;
                do {
                    attempts++;
                    if (new Random().nextDouble() < 0.8 && !commonTechStacks.isEmpty()) {
                        pick = commonTechStacks.get(new Random().nextInt(commonTechStacks.size()));
                    } else if (!otherTechStacks.isEmpty()) {
                        pick = otherTechStacks.get(new Random().nextInt(otherTechStacks.size()));
                    } else {
                        pick = techStacks.get(new Random().nextInt(techStacks.size()));
                    }
                } while (picked.contains(pick) && attempts < 10);

                if (!picked.contains(pick)) {
                    picked.add(pick);
                    int level = commonTechStacks.contains(pick) ? (new Random().nextInt(3) + 3) : (new Random().nextInt(4) + 1);
                    all.add(UserTechStack.builder().user(user).stack(pick).skillLevel(level).build());
                }
            }
        }

        if (!all.isEmpty()) userTechStackRepository.saveAll(all);
        log.info("Successfully created {} user tech stack relationships for {} users", all.size(), users.size());
    }

    /**
     * ⬇️ 위 파일에서 가져온: 사용자 경력 시드
     */
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

    /**
     * ⬇️ 위 파일에서 가져온: 사용자 학력 시드
     */
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

    /**
     * ⬇️ 위 파일에서 가져온: 팀원 리뷰 시드 (프로젝트 생성 후)
     */
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

                    int rating = random.nextInt(3) + 3; // 3~5
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

    // ─────────────────────────────────────────────────────
    // 밑 파일 고유 로직 유지: recommendation_history 테이블 업데이트, Like 생성 등
    // ─────────────────────────────────────────────────────

    private void updateRecommendationHistoryTable() {
        try {
            log.info("Updating recommendation_history table structure...");
            jdbcTemplate.execute("ALTER TABLE recommendation_history ADD COLUMN IF NOT EXISTS recommendation_type VARCHAR(10) DEFAULT 'PROJECT'");
            jdbcTemplate.execute("ALTER TABLE recommendation_history ADD COLUMN IF NOT EXISTS project_context_id BIGINT");
            jdbcTemplate.update("UPDATE recommendation_history SET recommendation_type = 'PROJECT' WHERE recommended_project_id IS NOT NULL AND recommendation_type IS NULL");
            jdbcTemplate.update("UPDATE recommendation_history SET recommendation_type = 'MEMBER' WHERE recommended_user_id IS NOT NULL AND recommendation_type IS NULL");
            log.info("✅ recommendation_history 테이블 업데이트 완료");
        } catch (Exception e) {
            log.warn("⚠️ recommendation_history 테이블 업데이트 실패: {}", e.getMessage());
        }
    }

    private void initializeLikeData() {
        log.info("Initializing smart like data based on tech stack similarity...");

        List<User> users = userRepository.findAll();
        List<ProjectRecruitment> projects = projectRecruitmentRepository.findAll();
        Random random = new Random();

        if (users.isEmpty() || projects.isEmpty()) {
            log.warn("Required data not found. Users: {}, Projects: {}", users.size(), projects.size());
            return;
        }

        List<User> likeUsers = users.size() > 20 ? users.subList(0, 20) : users;
        log.info("Using {} users (out of {}) for like generation", likeUsers.size(), users.size());

        List<Like> allLikes = new java.util.ArrayList<>();

        for (User user : likeUsers) {
            int likesPerUser = random.nextInt(8) + 1;
            List<ProjectRecruitment> likedProjects = new java.util.ArrayList<>();

            List<UserTechStack> userTechStacks = userTechStackRepository.findByUser(user);
            List<String> userTechNames = userTechStacks.stream()
                    .map(uts -> uts.getStack().getName().toLowerCase())
                    .toList();

            if (userTechNames.isEmpty()) continue;

            List<ProjectSimilarity> projectSimilarities = new java.util.ArrayList<>();

            for (ProjectRecruitment project : projects) {
                if (project.getTeamLeader().getUserId().equals(user.getUserId())) continue;

                List<ProjectTechStack> projectTechStacks = projectTechStackRepository.findByProject(project);
                List<String> projectTechNames = projectTechStacks.stream()
                        .map(pts -> pts.getTechStack().getName().toLowerCase())
                        .toList();

                if (projectTechNames.isEmpty()) continue;

                double similarity = calculateJaccardSimilarity(userTechNames, projectTechNames);
                double baseProbability = calculateLikeProbability(similarity);

                List<ProjectTechPart> projectTechParts = projectTechPartRepository.findByProject(project);
                UserProfile userProfile = userProfileRepository.findByUser(user).orElse(null);

                boolean techPartMatches = false;
                if (userProfile != null && userProfile.getTechPart() != null) {
                    techPartMatches = projectTechParts.stream()
                            .anyMatch(ptp -> ptp.getTechPart().getTechPartId()
                                    .equals(userProfile.getTechPart().getTechPartId()));
                }
                if (techPartMatches) baseProbability *= 1.5;

                long daysAgo = java.time.Duration.between(project.getCreatedAt(), LocalDateTime.now()).toDays();
                if (daysAgo < 30) baseProbability *= 1.3;
                else if (daysAgo < 90) baseProbability *= 1.1;

                baseProbability = Math.min(baseProbability, 0.8);

                projectSimilarities.add(new ProjectSimilarity(project, similarity, baseProbability));
            }

            projectSimilarities.sort((a, b) -> Double.compare(b.probability, a.probability));

            for (ProjectSimilarity ps : projectSimilarities) {
                if (likedProjects.size() >= likesPerUser) break;
                if (random.nextDouble() < ps.probability) {
                    likedProjects.add(ps.project);

                    Like like = Like.builder()
                            .user(user)
                            .targetId(ps.project.getProjectId())
                            .targetType(TargetType.PROJECT)
                            .build();

                    LocalDateTime likeCreatedAt = generateRandomDateBetween(ps.project.getCreatedAt(), LocalDateTime.now());
                    like.setCreatedAt(likeCreatedAt);

                    allLikes.add(like);
                }
            }

            if ((likeUsers.indexOf(user) + 1) % 10 == 0) {
                log.info("Processed {} users, generated {} likes so far...",
                        likeUsers.indexOf(user) + 1, allLikes.size());
            }
        }

        if (!allLikes.isEmpty()) {
            try {
                likeRepository.saveAll(allLikes);
            } catch (Exception e) {
                log.error("⚠️ 좋아요 배치 저장 실패: {}", e.getMessage());
                int success = 0;
                for (Like like : allLikes) {
                    try { likeRepository.save(like); success++; }
                    catch (Exception ex) { log.warn("개별 좋아요 저장 실패: {}", ex.getMessage()); }
                }
                log.info("개별 저장으로 {} 개의 좋아요 저장됨", success);
            }
        }

        log.info("✅ Successfully generated {} smart likes for {} users based on tech stack similarity",
                allLikes.size(), likeUsers.size());

        logLikeStatistics(allLikes.size(), likeUsers.size());
    }

    private double calculateJaccardSimilarity(List<String> userTechs, List<String> projectTechs) {
        if (userTechs.isEmpty() || projectTechs.isEmpty()) return 0.0;
        java.util.Set<String> userSet = new java.util.HashSet<>(userTechs);
        java.util.Set<String> projectSet = new java.util.HashSet<>(projectTechs);
        java.util.Set<String> intersection = new java.util.HashSet<>(userSet);
        intersection.retainAll(projectSet);
        java.util.Set<String> union = new java.util.HashSet<>(userSet);
        union.addAll(projectSet);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private double calculateLikeProbability(double jaccardSimilarity) {
        if (jaccardSimilarity >= 0.5) return 0.7;
        else if (jaccardSimilarity >= 0.3) return 0.5;
        else if (jaccardSimilarity >= 0.1) return 0.25;
        else if (jaccardSimilarity > 0) return 0.1;
        else return 0.02;
    }

    private LocalDateTime generateRandomDateBetween(LocalDateTime start, LocalDateTime end) {
        long startSeconds = start.toEpochSecond(java.time.ZoneOffset.UTC);
        long endSeconds = end.toEpochSecond(java.time.ZoneOffset.UTC);
        long randomSeconds = startSeconds + (long) (Math.random() * (endSeconds - startSeconds));
        return LocalDateTime.ofEpochSecond(randomSeconds, 0, java.time.ZoneOffset.UTC);
    }

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
