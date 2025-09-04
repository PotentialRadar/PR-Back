-- PotentialRadar seed data for PostgreSQL (mirrors TestDataInitializer)
-- Assumes schema is created by JPA/Hibernate as per entities.
-- Safe to run on a fresh DB. Re-running uses ON CONFLICT where possible.

-- Note: Extensions should be created manually in database:
-- CREATE EXTENSION IF NOT EXISTS citext;
-- CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------
-- Tech Parts (12)
-- ---------------------------------------------
INSERT INTO tech_part (name)
VALUES
  ('프론트엔드'), ('백엔드'), ('풀스택'), ('모바일'), ('데브옵스'),
  ('데이터사이언스'), ('AI/ML'), ('게임개발'), ('보안'), ('QA/테스터'),
  ('UI/UX디자인'), ('PM/기획')
ON CONFLICT (name) DO NOTHING;

-- ---------------------------------------------
-- Tech Stacks (subset incl. all used by projects/users)
-- ---------------------------------------------
WITH names(name) AS (
  SELECT UNNEST(ARRAY[
    'React','Vue.js','Angular','Next.js','Nuxt.js','Svelte','SvelteKit',
    'JavaScript','TypeScript','HTML5','CSS3',
    'Spring Boot','Spring Framework','Spring Security','Spring Data JPA','Spring Cloud',
    'Node.js','Express.js','Nest.js','Fastify','Django','Flask','FastAPI',
    'Java','Python','C#','Go','Rust','Kotlin','Scala','Clojure','Ruby','PHP','Swift','Objective-C','Dart','R',
    'PostgreSQL','MySQL','SQLite','MongoDB','Redis','Elasticsearch',
    'AWS','Google Cloud Platform','Azure',
    'Docker','Kubernetes','Helm','Jenkins',
    'TensorFlow','PyTorch','Keras','Scikit-learn','Pandas','NumPy',
    'Selenium','Playwright','Cypress','Jest',
    'Unity','Unreal Engine','Blender',
    'Flutter','React Native','Android','iOS','Firebase','SQLite','CoreData',
    'Socket.io','Web3.js','Solidity','pgAdmin','Linux'
  ])
)
INSERT INTO tech_stack(name)
SELECT name FROM names
ON CONFLICT (name) DO NOTHING;

-- ---------------------------------------------
-- Users (100) and Profiles
-- ---------------------------------------------
WITH u(i, email, provider) AS (
  SELECT gs,
         format('user%03s@naver.com', to_char(gs,'FM000')),
         CASE WHEN (gs-1)%5 < 3 THEN 'EMAIL' WHEN (gs-1)%5 = 3 THEN 'GOOGLE' ELSE 'KAKAO' END
  FROM generate_series(1,100) AS gs
), nick AS (
  SELECT i,
         (ARRAY[
           '코딩마스터001','개발자김철수','프론트엔드박영희','백엔드이민수','풀스택홍길동',
           'ReactDeveloper','SpringMaster','NodeJSExpert','VueDeveloper','PythonGuru',
           'JavaScriptNinja','SQLMaster','CSSWizard','HTMLCoder','TypeScriptDev',
           'AndroidDev','iOSExpert','FlutterCoder','ReactNativeDev','SwiftProgrammer',
           'KotlinDev','DevOpsEngineer','DockerMaster','KubernetesExpert','AWSSpecialist',
           'DataScientist','MLEngineer','AIResearcher','BigDataAnalyst','TensorFlowDev',
           'GameDeveloper','UnityExpert','UnrealCoder','GameDesigner','GraphicsProgrammer',
           'SecurityExpert','PenetrationTester','CybersecurityAnalyst','SecurityArchitect','CryptographyExpert',
           'QAEngineer','TestAutomation','PerformanceTester','ManualTester','QualityAssurance',
           'UIDesigner','UXResearcher','ProductDesigner','InteractionDesigner','VisualDesigner',
           'ProductManager','ProjectManager','TechLead','ScrumMaster','BusinessAnalyst',
           'SystemArchitect','DatabaseAdmin','NetworkEngineer','CloudArchitect','InfraEngineer',
           'JavaDeveloper','CSharpDev','GoDeveloper','RustProgrammer','PhpDeveloper',
           'RubyDeveloper','ScalaDeveloper','ElixirCoder','HaskellDev','ClojureCoder',
           'BlockchainDev','SmartContractDev','Web3Developer','DAppDeveloper','NFTCreator',
           'IoTDeveloper','EmbeddedEngineer','FirmwareDev','HardwareEngineer','RoboticsEngineer',
           'ARDeveloper','VRDeveloper','XREngineer','MetaverseDev','3DGraphicsDev',
           'QuantumComputing','CloudNativeDev','MicroservicesArch','ServerlessExpert','EdgeComputingDev',
           'TechEvangelist','DeveloperAdvocate','TechnicalWriter','CodeReviewer','OpenSourceMaintainer',
           'CommunityManager','TechRecruiter','StartupFounder','TechConsultant','DigitalNomad'
         ])[i] AS nickname
  FROM u
), ins_users AS (
  INSERT INTO users (email, password, nickname, provider, provider_user_id, profile_image)
  SELECT 
    u.email,
    CASE WHEN u.provider = 'EMAIL' THEN crypt('1234', gen_salt('bf', 10)) ELSE NULL END,
    n.nickname,
    u.provider,
    CASE 
      WHEN u.provider = 'EMAIL' THEN NULL
      WHEN u.provider = 'GOOGLE' THEN 'google_' || (floor(random()*900000000)+100000000)::bigint
      ELSE 'kakao_'  || (floor(random()*900000000)+100000000)::bigint
    END,
    format('https://example.com/profile/%03s.jpg', to_char(ceil(random()*12)::int,'FM000'))
  FROM u
  JOIN nick n USING (i)
  ON CONFLICT (email) DO NOTHING
  RETURNING user_id, email, provider, nickname
), all_users AS (
  SELECT u.user_id, u.email, u.provider, u.nickname
  FROM ins_users u
  UNION ALL
  SELECT user_id, email, provider, nickname
  FROM users
  WHERE email LIKE 'user___@naver.com'
), all_users_rn AS (
  SELECT au.*, row_number() OVER (ORDER BY au.user_id) AS rn
  FROM all_users au
), tp_ct AS (
  SELECT count(*)::int AS c FROM tech_part
)
INSERT INTO user_profile (
  user_id, tech_part_id, bio, job_title, phone, github_url, linkedin_url, website_url,
  is_portfolio_open, is_contact_open, is_search_open, reputation_score, review_count, experience_range
)
SELECT 
  aur.user_id,
  tp.tech_part_id,
  CASE WHEN (aur.rn-1) % 3 = 0 THEN NULL
       WHEN (aur.rn-1) % 5 = 0
         THEN '안녕하세요! '||aur.nickname||'입니다. 다양한 프로젝트 경험을 통해 성장하고 있습니다.'
       ELSE '열정적인 개발자 '||aur.nickname||'입니다. 새로운 기술 학습과 협업을 좋아합니다. 함께 멋진 프로젝트를 만들어봅시다!'
  END AS bio,
  CASE tp.name
    WHEN '프론트엔드' THEN 'Frontend Developer'
    WHEN '백엔드'   THEN 'Backend Developer'
    WHEN '풀스택'   THEN 'Full Stack Developer'
    WHEN '모바일'   THEN 'Mobile Developer'
    WHEN '데브옵스' THEN 'DevOps Engineer'
    WHEN '데이터사이언스' THEN 'Data Scientist'
    WHEN 'AI/ML'   THEN 'AI/ML Engineer'
    WHEN '게임개발' THEN 'Game Developer'
    WHEN '보안'     THEN 'Security Expert'
    WHEN 'QA/테스터' THEN 'QA Engineer'
    WHEN 'UI/UX디자인' THEN 'UI/UX Designer'
    WHEN 'PM/기획'  THEN 'Product Manager'
    ELSE 'Software Developer'
  END AS job_title,
  CASE WHEN (aur.rn-1) % 5 = 0 THEN NULL
       ELSE '010-'||LPAD(((aur.rn)*37 % 10000)::text, 4, '0')
            ||'-'||LPAD(((aur.rn)*73 % 10000)::text, 4, '0') END AS phone,
  CASE WHEN (aur.rn-1) % 3 = 0 THEN NULL
       ELSE 'https://github.com/'||regexp_replace(lower(aur.nickname),'[^a-z0-9]','','g') END AS github_url,
  CASE WHEN (aur.rn-1) % 4 = 0 THEN NULL
       ELSE 'https://linkedin.com/in/'||regexp_replace(lower(aur.nickname),'[^a-z0-9]','-','g') END AS linkedin_url,
  CASE WHEN (aur.rn-1) % 6 = 0 THEN NULL
       ELSE 'https://'||regexp_replace(lower(aur.nickname),'[^a-z0-9]','','g')||'.dev' END AS website_url,
  CASE WHEN (aur.rn-1) % 3 = 1 THEN TRUE ELSE FALSE END AS is_portfolio_open,
  CASE WHEN (aur.rn-1) % 4 = 1 THEN TRUE ELSE FALSE END AS is_contact_open,
  CASE WHEN (aur.rn-1) % 2 = 1 THEN TRUE ELSE FALSE END AS is_search_open,
  ROUND(((random()*4.5 + 0.5))::numeric, 2) AS reputation_score,
  (random()*50)::int AS review_count,
  (ARRAY['FRESHER','LT_1','Y1_3','Y3_5','Y5_10','GE_10','ETC'])[(aur.rn-1) % 7 + 1]
FROM all_users_rn aur
JOIN tp_ct c ON TRUE
JOIN LATERAL (
  SELECT tp.tech_part_id, tp.name
  FROM tech_part tp
  ORDER BY tp.tech_part_id
  LIMIT 1 OFFSET ((aur.rn-1) % c.c)
) tp ON TRUE
ON CONFLICT (user_id) DO NOTHING;

-- ---------------------------------------------
-- User Tech Stacks: 3~7 random stacks per user
-- ---------------------------------------------
INSERT INTO user_tech_stack (user_id, stack_id, skill_level)
SELECT u.user_id, s.tech_stack_id, (floor(random()*5)+1)::int
FROM users u
JOIN LATERAL (
  SELECT ts.tech_stack_id
  FROM tech_stack ts
  ORDER BY random()
  LIMIT (3 + (floor(random()*5))::int)  -- 3..7
) s ON TRUE
ON CONFLICT ON CONSTRAINT uk_user_tech_stack_user_stack DO NOTHING;

-- ---------------------------------------------
-- User Experiences: ~0..3 per user with time windows
-- ---------------------------------------------
WITH base AS (
  SELECT u.user_id,
         g.n AS n,
         random() AS r
  FROM users u
  JOIN LATERAL generate_series(1,3) g(n) ON TRUE
), sel AS (
  SELECT *,
         CASE WHEN n=1 AND r < 0.4 THEN TRUE ELSE FALSE END AS is_current
  FROM base
)
INSERT INTO user_experience (user_id, company_name, department, start_date, end_date, is_current, summary)
SELECT 
  s.user_id,
  (ARRAY['네이버','카카오','라인','쿠팡','배달의민족','토스','당근마켓','야놀자','NHN','넥슨','엔씨소프트','넷마블','크래프톤','스마일게이트','위메프','11번가','SK텔레콤','KT','LG유플러스','삼성SDS','LG CNS','포스코DX','현대오토에버','우아한형제들','마켓컬리','직방','집닥','29CM','무신사','브랜디','에이블리','Google','Microsoft','Amazon','Meta','Apple','Netflix','Uber','Airbnb'])
    [ (floor(random()*38)+1)::int ],
  (ARRAY['개발팀','프론트엔드팀','백엔드팀','모바일팀','데브옵스팀','인프라팀','데이터팀','AI팀','ML팀','보안팀','QA팀','제품팀','기획팀','디자인팀'])[(floor(random()*14)+1)::int],
  CASE WHEN s.is_current THEN (current_date - ((floor(random()*36)+6)||' months')::interval)::date
       ELSE (current_date - ((6 + s.n*12 + floor(random()*24))||' months')::interval)::date END AS start_date,
  CASE WHEN s.is_current THEN NULL::date
       ELSE (current_date - ((6 + s.n*12 + floor(random()*24))||' months')::interval)::date END AS end_date,
  s.is_current,
  (ARRAY['웹 애플리케이션 개발 및 유지보수 담당','RESTful API 설계 및 구현','프론트엔드 컴포넌트 개발 및 최적화','모바일 앱 개발 및 성능 개선','데이터베이스 설계 및 쿼리 최적화','CI/CD 파이프라인 구축 및 관리','마이크로서비스 아키텍처 설계','사용자 경험 개선을 위한 UI/UX 개발','대용량 트래픽 처리 시스템 개발','클라우드 인프라 구축 및 운영'])[(floor(random()*10)+1)::int]
FROM sel s
WHERE random() < CASE WHEN s.n IN (1,2) THEN 0.75 ELSE 0.5 END;

-- ---------------------------------------------
-- User Educations: 1..2 per user
-- ---------------------------------------------
WITH base AS (
  SELECT u.user_id, n
  FROM users u
  JOIN LATERAL generate_series(1,2) g(n) ON TRUE
)
INSERT INTO user_education (user_id, institution, program, start_date, end_date, is_current)
SELECT 
  b.user_id,
  (ARRAY['서울대학교','연세대학교','고려대학교','성균관대학교','한양대학교','중앙대학교','경희대학교','서강대학교','이화여자대학교','건국대학교','동국대학교','홍익대학교','숭실대학교','국민대학교','KAIST','포항공과대학교','부산대학교','경북대학교','전남대학교','충남대학교'])[(floor(random()*20)+1)::int],
  (ARRAY['컴퓨터공학과','소프트웨어학과','정보통신공학과','전자공학과','컴퓨터과학과','데이터사이언스학과','인공지능학과','게임학과','정보보호학과','산업공학과','시각디자인학과','경영학과','경영정보학과','멀티미디어학과','수학과'])[(floor(random()*15)+1)::int]
  || ' ' || CASE WHEN b.n=1 THEN '학사' ELSE '석사' END,
  (current_date - ((4 + floor(random()*3))||' years')::interval)::date - CASE WHEN b.n=1 THEN interval '4 years' ELSE interval '2 years' END,
  (current_date - ((2 + floor(random()*7))||' years')::interval)::date,
  FALSE
FROM base b
WHERE (b.n=1) OR (random() < 0.2);

-- ---------------------------------------------
-- Projects (50)
-- ---------------------------------------------
WITH base AS (
  SELECT gs AS i,
         (SELECT user_id FROM users ORDER BY random() LIMIT 1) AS team_leader_id,
         current_date AS today
  FROM generate_series(1,50) gs
), dates AS (
  SELECT i, team_leader_id,
         (today + ((10 + floor(random()*30))||' days')::interval)::date AS recruit_deadline,
         today
  FROM base
), dd AS (
  SELECT i, team_leader_id, recruit_deadline,
         (recruit_deadline + ((1 + floor(random()*14))||' days')::interval)::date AS start_date
  FROM dates
), ddd AS (
  SELECT i, team_leader_id, recruit_deadline, start_date,
         (start_date + ((30 + floor(random()*120))||' days')::interval)::date AS end_date
  FROM dd
), ins_proj AS (
  INSERT INTO project_recruitment (
    team_leader_id, title, description, recruit_deadline, start_date, end_date, status, view_count, recruit_count
  )
  SELECT 
    d.team_leader_id,
    (ARRAY[
      'React 기반 쇼핑몰 플랫폼 개발',
      'AI 챗봇 서비스 구축',
      '모바일 피트니스 트래킹 앱',
      '블록체인 기반 NFT 마켓플레이스',
      '실시간 스트리밍 플랫폼',
      'IoT 스마트홈 제어 시스템',
      '온라인 교육 플랫폼',
      '소셜 네트워킹 서비스',
      '부동산 중개 플랫폼',
      '음식 주문 배달 앱',
      '게임 커뮤니티 사이트',
      '헬스케어 관리 시스템',
      '여행 계획 및 예약 서비스',
      '주식 투자 분석 도구',
      '온라인 쇼핑몰 CMS',
      'AR/VR 체험 플랫폼',
      '클라우드 파일 저장소',
      '팀 협업 툴',
      '날씨 예보 서비스',
      '음악 스트리밍 플랫폼',
      '언어 학습 애플리케이션',
      '레시피 공유 커뮤니티',
      '중고거래 마켓플레이스',
      '온라인 도서관 시스템',
      '펫샵 관리 플랫폼',
      '부동산 투자 분석 도구',
      '카페 주문 시스템',
      '택시 호출 서비스',
      '온라인 갤러리 플랫폼',
      '스마트 농업 관리 시스템',
      '의료진 스케줄 관리',
      '영화 리뷰 커뮤니티',
      '온라인 옥션 플랫폼',
      '디지털 명함 서비스',
      'QR코드 메뉴 시스템',
      '스마트 주차장 관리',
      '온라인 투표 시스템',
      '크라우드펀딩 플랫폼',
      '온라인 상담 서비스',
      '스포츠 경기 중계 앱',
      '디지털 지갑 서비스',
      '온라인 심리상담 플랫폼',
      '스마트 물류 관리 시스템',
      '온라인 법률 상담 서비스',
      '디지털 마케팅 도구',
      '온라인 회계 관리 시스템',
      '스마트 시티 모니터링',
      '온라인 예약 관리 시스템',
      'AI 기반 추천 시스템',
      '블록체인 투표 플랫폼'
    ])[(d.i % 50) + 1],
    (ARRAY[
      '사용자 친화적인 UI/UX로 최고의 쇼핑 경험을 제공하는 이커머스 플랫폼을 개발합니다.',
      '자연어 처리 기술을 활용한 지능형 챗봇으로 고객 서비스를 자동화합니다.',
      '건강한 생활을 위한 운동 기록과 목표 관리가 가능한 모바일 앱을 제작합니다.',
      'NFT 거래와 창작자 지원을 위한 안전하고 투명한 블록체인 마켓플레이스를 구축합니다.',
      '고화질 실시간 스트리밍과 인터랙티브 기능을 제공하는 플랫폼을 개발합니다.',
      '스마트홈 기기들을 통합 제어할 수 있는 IoT 솔루션을 구현합니다.',
      '온라인 강의와 학습 관리가 통합된 종합 교육 플랫폼을 제작합니다.',
      '사용자 간 소통과 네트워킹을 촉진하는 혁신적인 소셜 플랫폼을 개발합니다.',
      '부동산 거래의 투명성을 높이고 중개 과정을 간소화하는 플랫폼을 구축합니다.',
      '빠른 주문과 배달 추적이 가능한 음식 배달 서비스 앱을 제작합니다.',
      '게임 애호가들을 위한 정보 공유와 커뮤니티 기능을 제공하는 사이트를 개발합니다.',
      '환자와 의료진을 연결하는 디지털 헬스케어 관리 시스템을 구현합니다.',
      '여행 계획부터 예약까지 원스톱으로 처리할 수 있는 여행 서비스를 개발합니다.',
      'AI 기반 주식 분석과 투자 전략 수립을 도와주는 금융 도구를 제작합니다.',
      '온라인 쇼핑몰 운영자를 위한 통합 관리 시스템을 개발합니다.',
      '가상현실과 증강현실 콘텐츠를 체험할 수 있는 플랫폼을 구축합니다.',
      '안전하고 편리한 클라우드 기반 파일 저장 및 공유 서비스를 제작합니다.',
      '팀 프로젝트의 효율성을 높이는 협업 도구를 개발합니다.',
      '정확한 날씨 정보와 생활 지수를 제공하는 기상 서비스를 구현합니다.',
      '개인화된 음악 추천과 고음질 스트리밍을 제공하는 플랫폼을 개발합니다.',
      '효과적인 언어 학습을 위한 인터랙티브 애플리케이션을 제작합니다.',
      '요리 레시피 공유와 요리 팁 교환이 가능한 커뮤니티를 구축합니다.',
      '안전한 중고거래를 위한 신뢰성 높은 마켓플레이스를 개발합니다.',
      '디지털 도서 관리와 대여 시스템을 통합한 온라인 도서관을 구현합니다.',
      '펫샵 운영과 고객 관리를 효율화하는 통합 플랫폼을 제작합니다.',
      '부동산 투자 수익률 분석과 시장 동향을 제공하는 도구를 개발합니다.',
      '카페 주문과 결제를 간편화하는 스마트 오더 시스템을 구축합니다.',
      '빠르고 안전한 택시 호출과 경로 최적화 서비스를 제작합니다.',
      '디지털 아트 작품 전시와 판매가 가능한 온라인 갤러리를 개발합니다.',
      'IoT 센서 기반 스마트 농업 관리 및 모니터링 시스템을 구현합니다.',
      '병원 의료진의 효율적인 스케줄 관리를 위한 시스템을 개발합니다.',
      '영화 리뷰와 평점 공유가 가능한 영화 커뮤니티 플랫폼을 제작합니다.',
      '투명하고 공정한 온라인 경매 시스템을 구축합니다.',
      'QR코드 기반 디지털 명함과 네트워킹 서비스를 개발합니다.',
      '레스토랑 테이블에서 QR코드로 주문할 수 있는 무접촉 메뉴 시스템을 구현합니다.',
      'IoT 기반 스마트 주차장 관리와 예약 시스템을 제작합니다.',
      '안전하고 투명한 온라인 투표 및 설문 플랫폼을 개발합니다.',
      '창의적인 프로젝트를 위한 크라우드펀딩 플랫폼을 구축합니다.',
      '전문가와 고객을 연결하는 온라인 상담 서비스를 제작합니다.',
      '실시간 스포츠 경기 중계와 통계를 제공하는 모바일 앱을 개발합니다.',
      '암호화폐와 디지털 자산 관리를 위한 안전한 지갑 서비스를 구현합니다.',
      '온라인 심리상담과 정신건강 관리를 지원하는 플랫폼을 제작합니다.',
      'AI 기반 물류 최적화와 배송 관리 시스템을 개발합니다.',
      '법률 전문가와 고객을 연결하는 온라인 법률 상담 플랫폼을 구축합니다.',
      '마케팅 캠페인 관리와 성과 분석을 위한 올인원 도구를 제작합니다.',
      '중소기업을 위한 클라우드 기반 회계 관리 시스템을 개발합니다.',
      '도시 인프라 모니터링과 관리를 위한 스마트 시티 플랫폼을 구현합니다.',
      '다양한 업종의 예약 관리를 통합하는 범용 예약 시스템을 제작합니다.',
      '사용자 행동 분석 기반 개인화 추천 시스템을 개발합니다.',
      '블록체인 기술을 활용한 투명하고 조작 불가능한 투표 플랫폼을 구축합니다.'
    ])[(d.i % 50) + 1],
    d.recruit_deadline,
    d.start_date,
    d.end_date,
    (ARRAY['RECRUITING','IN_PROGRESS','COMPLETED'])[(floor(random()*3)+1)::int],
    (floor(random()*500))::int,
    (3 + floor(random()*5))::int
  FROM ddd d
  RETURNING project_id, team_leader_id
)
-- Project leader member row
INSERT INTO project_member (project_id, user_id, role, tech_part)
SELECT p.project_id, p.team_leader_id, 'LEADER', 
  (ARRAY['프론트엔드', '백엔드', '풀스택', '모바일', '데브옵스', 'AI/ML', 'UI/UX디자인', 'PM/기획'])[(p.project_id % 8) + 1]
FROM ins_proj p;

-- Project Tech Parts (1..3 random per project)
INSERT INTO project_tech_part (project_id, tech_part_id, recruit_count)
SELECT p.project_id, tp.tech_part_id, (1 + floor(random()*3))::int
FROM project_recruitment p
JOIN LATERAL (
  SELECT tech_part_id FROM tech_part ORDER BY random() LIMIT (1 + floor(random()*3))::int
) tp ON TRUE
ON CONFLICT DO NOTHING;

-- Project Tech Stacks: 5..6 random entries per project
INSERT INTO project_tech_stack (project_id, tech_stack_id, recruit_count)
SELECT p.project_id, ts.tech_stack_id, (1 + floor(random()*2))::int
FROM project_recruitment p
JOIN LATERAL (
  SELECT tech_stack_id FROM tech_stack ORDER BY random() LIMIT (5 + floor(random()*2))::int
) ts ON TRUE
ON CONFLICT ON CONSTRAINT uq_project_tech_stack DO NOTHING;

-- Additional project members: 2..5 per project, unique per project, not leader
INSERT INTO project_member (project_id, user_id, role, tech_part)
SELECT p.project_id, m.user_id, 'MEMBER', 
  (ARRAY['프론트엔드', '백엔드', '풀스택', '모바일', '데브옵스', 'AI/ML', '게임개발', '보안', 'QA/테스터', 'UI/UX디자인', 'PM/기획', '데이터사이언스'])[(m.user_id % 12) + 1]
FROM project_recruitment p
JOIN LATERAL (
  SELECT u.user_id, row_number() OVER (ORDER BY random()) as rn
  FROM users u
  WHERE u.user_id <> p.team_leader_id
  ORDER BY random()
  LIMIT (2 + floor(random()*4))::int
) m ON TRUE
ON CONFLICT DO NOTHING;

-- ---------------------------------------------
-- Team Member Reviews (80% probability for each pair)
-- ---------------------------------------------
INSERT INTO team_member_review (project_id, reviewer_id, reviewee_id, rating, comment, created_at)
SELECT 
  pm1.project_id,
  pm1.user_id AS reviewer_id,
  pm2.user_id AS reviewee_id,
  (3 + floor(random()*3))::int AS rating,
  CASE WHEN (3 + floor(random()*3))::int >= 4 THEN (
       (ARRAY[
         '정말 훌륭한 팀원이었습니다! 프로젝트에 큰 도움이 되었어요.',
         '커뮤니케이션이 원활하고 책임감이 강한 개발자입니다.',
         '기술적 역량이 뛰어나고 팀워크가 좋습니다.',
         '문제 해결 능력이 뛰어나고 적극적으로 참여해주셨습니다.',
         '코드 품질이 우수하고 일정 관리를 잘 해주셨어요.',
         '창의적인 아이디어를 많이 제안해주시고 실행력이 좋습니다.',
         '다른 팀원들과의 협업이 매우 원활했습니다.',
         '기한을 잘 지키시고 꼼꼼하게 작업해주셨습니다.',
         'React 컴포넌트 설계를 정말 잘하시네요! 재사용성이 뛰어났어요.',
         'API 설계와 문서화가 깔끔했습니다. 덕분에 작업이 수월했어요.',
         'UI/UX에 대한 센스가 뛰어나시고 사용자 경험을 잘 고려하세요.',
         '데이터베이스 최적화 작업이 인상적이었습니다.',
         'Git 사용법이 정말 깔끔하시고 코드리뷰도 도움이 많이 됐어요.',
         '테스트 코드 작성을 꼼꼼히 해주셔서 버그가 거의 없었네요.',
         '새로운 기술 도입에 적극적이시고 학습 능력이 뛰어나요.',
         '프로젝트 일정 관리와 이슈 트래킹을 체계적으로 해주셨어요.',
         '백엔드 아키텍처 설계가 깔끔하고 확장성을 잘 고려하셨네요.',
         '프론트엔드 성능 최적화 작업이 정말 대단했습니다!',
         '데브옵스 지식이 풍부하시고 배포 자동화를 잘 구축해주셨어요.',
         '모바일 반응형 디자인 구현 실력이 뛰어나시네요!'
       ])[(floor(random()*20)+1)::int]
  ) ELSE (
       (ARRAY[
         '전반적으로 무난한 팀원이었습니다.',
         '맡은 역할을 잘 수행해주셨습니다.',
         '프로젝트에 성실하게 참여해주셨어요.',
         '기본기가 탄탄한 개발자입니다.',
         '주어진 업무를 차근차근 완수해주셨습니다.',
         '소통이 조금 아쉬웠지만 결과물은 만족스러웠어요.',
         '일정 관리가 조금 아쉬웠지만 열심히 해주셨어요.',
         '초기에는 어려움이 있었지만 점점 나아졌습니다.',
         '기술적 부분에서 더 성장할 여지가 있어 보여요.',
         '다음에는 더 적극적으로 참여해주시면 좋겠어요.',
         '코드 리뷰 참여가 조금 아쉬웠어요.',
         '문서화 작업이 부족했지만 개발 실력은 괜찮아요.',
         '시간 관리가 아쉬웠지만 마지막에 잘 마무리해주셨어요.',
         '커뮤니케이션 스킬을 더 키우시면 좋을 것 같아요.',
         '기술 선택에 대한 고민이 더 필요해 보여요.'
       ])[(floor(random()*15)+1)::int]
  ) END AS comment,
  current_timestamp
FROM project_member pm1
JOIN project_member pm2 
  ON pm1.project_id = pm2.project_id 
  AND pm1.user_id <> pm2.user_id
WHERE random() < 0.8
ON CONFLICT (project_id, reviewer_id, reviewee_id) DO NOTHING;

-- ---------------------------------------------
-- Portfolio Projects (사용자가 참여한 프로젝트 중 랜덤하게 포트폴리오 등록)
-- ---------------------------------------------
INSERT INTO portfolio_project (user_id, project_id, created_at)
SELECT 
  pm.user_id,
  pm.project_id,
  current_timestamp
FROM project_member pm
WHERE random() < 0.4  -- 40% 확률로 포트폴리오에 등록
ON CONFLICT (user_id, project_id) DO NOTHING;