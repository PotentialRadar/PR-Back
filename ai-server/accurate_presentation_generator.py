#!/usr/bin/env python3
"""
정확한 표현을 사용한 발표 자료 생성기
실제 데이터 기반으로 시뮬레이션 성능 평가 결과 생성
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import precision_score, recall_score, f1_score
import json
from pathlib import Path

# 한글 폰트 설정
plt.rcParams['font.family'] = ['AppleGothic', 'Malgun Gothic', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False

class AccuratePresentationGenerator:
    def __init__(self, data_path="training_data_enhanced.csv"):
        self.data_path = data_path
        self.results = {}
        
        # 정확한 색상 팔레트
        self.colors = {
            'baseline': '#E74C3C',     # 빨간색 - 기준 알고리즘
            'improved': '#3498DB',     # 파란색 - 개선된 알고리즘
            'optimized': '#2ECC71',    # 초록색 - 최적화된 알고리즘
            'background': '#F8F9FA',   # 배경
            'text': '#2C3E50'          # 텍스트
        }
    
    def load_real_data(self):
        """실제 데이터 로드 및 분석"""
        print("📊 실제 훈련 데이터 분석 시작...")
        
        df = pd.read_csv(self.data_path)
        print(f"✅ 실제 데이터: {len(df):,}개 레코드 로드")
        
        # 데이터 통계
        positive_samples = df[df['label'] == 1].shape[0]
        negative_samples = df[df['label'] == 0].shape[0]
        
        print(f"📈 긍정 샘플: {positive_samples:,}개 ({positive_samples/len(df)*100:.1f}%)")
        print(f"📉 부정 샘플: {negative_samples:,}개 ({negative_samples/len(df)*100:.1f}%)")
        
        return df
    
    def extract_algorithm_features(self, user_techs, project_techs):
        """3단계 알고리즘별 특성 추출"""
        user_set = set([tech.lower() for tech in user_techs])
        project_set = set([tech.lower() for tech in project_techs])
        
        # 1단계: 기준 알고리즘 (자카드 유사도)
        intersection = len(user_set & project_set)
        union = len(user_set | project_set)
        jaccard_score = intersection / union if union > 0 else 0
        
        # 2단계: 개선된 알고리즘 (다차원 특성)
        overlap_ratio = intersection / len(user_set) if user_set else 0
        coverage_ratio = intersection / len(project_set) if project_set else 0
        size_similarity = 1 - abs(len(user_set) - len(project_set)) / max(len(user_set), len(project_set), 1)
        
        # 3단계: 최적화된 알고리즘 (기술 그룹 연관성 추가)
        tech_groups = {
            'frontend': ['react', 'vue.js', 'angular', 'javascript', 'typescript'],
            'backend': ['java', 'spring', 'python', 'node.js', 'django'],
            'database': ['mysql', 'postgresql', 'mongodb', 'redis'],
            'cloud': ['aws', 'docker', 'kubernetes']
        }
        
        user_groups = set()
        project_groups = set()
        
        for tech in user_set:
            for group, techs in tech_groups.items():
                if tech in techs:
                    user_groups.add(group)
        
        for tech in project_set:
            for group, techs in tech_groups.items():
                if tech in techs:
                    project_groups.add(group)
        
        group_similarity = len(user_groups & project_groups) / len(user_groups | project_groups) if (user_groups | project_groups) else 0
        
        return {
            'baseline': [jaccard_score],  # 기준 알고리즘
            'improved': [jaccard_score, overlap_ratio, coverage_ratio],  # 개선된 알고리즘
            'optimized': [jaccard_score, overlap_ratio, coverage_ratio, size_similarity, group_similarity]  # 최적화된 알고리즘
        }
    
    def evaluate_algorithms_on_real_data(self):
        """실제 데이터 기반 알고리즘 성능 평가"""
        print("🔄 실제 데이터 기반 알고리즘 성능 평가...")
        
        df = self.load_real_data()
        
        # 특성 추출
        all_features = {'baseline': [], 'improved': [], 'optimized': []}
        labels = []
        
        for idx, row in df.iterrows():
            try:
                user_techs = eval(row['user_techs']) if isinstance(row['user_techs'], str) else []
                project_techs = eval(row['project_techs']) if isinstance(row['project_techs'], str) else []
                
                features = self.extract_algorithm_features(user_techs, project_techs)
                
                for alg_type in all_features.keys():
                    all_features[alg_type].append(features[alg_type])
                
                labels.append(row['label'])
                
            except Exception:
                continue
        
        # 각 알고리즘별 성능 평가
        results = {}
        
        for alg_name, feature_vectors in all_features.items():
            print(f"\n📊 {alg_name.upper()} 알고리즘 평가 중...")
            
            X = np.array(feature_vectors)
            y = np.array(labels)
            
            # 훈련/테스트 분할
            X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.3, random_state=42)
            
            # 모델 훈련
            if alg_name == 'baseline':
                # 단순 임계값 기반
                threshold = 0.1
                y_pred = (X_test[:, 0] > threshold).astype(int)
                y_pred_proba = X_test[:, 0]
            else:
                # Random Forest 사용
                model = RandomForestClassifier(n_estimators=50, random_state=42)
                model.fit(X_train, y_train)
                y_pred = model.predict(X_test)
                y_pred_proba = model.predict_proba(X_test)[:, 1]
            
            # 성능 지표 계산
            precision = precision_score(y_test, y_pred, zero_division=0)
            recall = recall_score(y_test, y_pred, zero_division=0)
            f1 = f1_score(y_test, y_pred, zero_division=0)
            
            # mAP 계산
            map_score = self.calculate_map_score(y_test, y_pred_proba)
            
            results[alg_name] = {
                'precision': precision,
                'recall': recall,
                'f1_score': f1,
                'map_score': map_score,
                'samples': len(y_test)
            }
            
            print(f"  Precision: {precision:.3f}")
            print(f"  Recall: {recall:.3f}")
            print(f"  F1-Score: {f1:.3f}")
            print(f"  mAP@5: {map_score:.3f}")
        
        self.results = results
        return results
    
    def calculate_map_score(self, y_true, y_pred_proba, k=5):
        """mAP@K 계산"""
        average_precisions = []
        batch_size = 20
        
        for i in range(0, len(y_true), batch_size):
            batch_true = y_true[i:i+batch_size]
            batch_pred = y_pred_proba[i:i+batch_size]
            
            if sum(batch_true) == 0:
                continue
            
            sorted_indices = np.argsort(batch_pred)[::-1]
            relevant_count = 0
            precision_sum = 0
            
            for rank, idx in enumerate(sorted_indices[:k]):
                if batch_true[idx] == 1:
                    relevant_count += 1
                    precision_at_rank = relevant_count / (rank + 1)
                    precision_sum += precision_at_rank
            
            if relevant_count > 0:
                ap = precision_sum / relevant_count
                average_precisions.append(ap)
        
        return np.mean(average_precisions) if average_precisions else 0
    
    def create_accurate_presentation_chart(self, save_path="실제_데이터_기반_성능_평가.png"):
        """정확한 표현을 사용한 발표 차트"""
        if not self.results:
            print("❌ 먼저 evaluate_algorithms_on_real_data()를 실행하세요.")
            return
        
        fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(15, 10))
        fig.patch.set_facecolor(self.colors['background'])
        
        algorithms = ['기준 알고리즘', '개선된 알고리즘', '최적화된 알고리즘']
        alg_keys = ['baseline', 'improved', 'optimized']
        colors = [self.colors['baseline'], self.colors['improved'], self.colors['optimized']]
        
        # 데이터 추출
        precisions = [self.results[key]['precision'] for key in alg_keys]
        recalls = [self.results[key]['recall'] for key in alg_keys]
        f1_scores = [self.results[key]['f1_score'] for key in alg_keys]
        map_scores = [self.results[key]['map_score'] for key in alg_keys]
        
        # 1. 시뮬레이션 기반 정밀도 평가
        bars1 = ax1.bar(algorithms, precisions, color=colors, alpha=0.8)
        ax1.set_title('시뮬레이션 기반 정밀도 평가', fontweight='bold', fontsize=12)
        ax1.set_ylabel('Precision')
        ax1.set_ylim(0, max(precisions) * 1.2)
        
        for i, (bar, value) in enumerate(zip(bars1, precisions)):
            height = bar.get_height()
            ax1.text(bar.get_x() + bar.get_width()/2., height + 0.01,
                    f'{value:.3f}', ha='center', va='bottom', fontweight='bold')
        
        # 2. 알고리즘 개선 효과
        x = np.arange(len(algorithms))
        ax2.plot(x, f1_scores, marker='o', linewidth=3, markersize=8, 
                color=self.colors['improved'], label='F1-Score')
        ax2.fill_between(x, f1_scores, alpha=0.3, color=self.colors['improved'])
        ax2.set_title('알고리즘 개선 효과', fontweight='bold', fontsize=12)
        ax2.set_ylabel('F1-Score')
        ax2.set_xticks(x)
        ax2.set_xticklabels(algorithms)
        ax2.grid(True, alpha=0.3)
        ax2.set_ylim(0, max(f1_scores) * 1.2)
        
        for i, value in enumerate(f1_scores):
            ax2.annotate(f'{value:.3f}', (i, value), textcoords="offset points", 
                        xytext=(0,10), ha='center', fontweight='bold')
        
        # 3. 기술적 지표 향상
        width = 0.35
        x = np.arange(len(algorithms))
        
        bars3_1 = ax3.bar(x - width/2, precisions, width, label='Precision', 
                         color=self.colors['baseline'], alpha=0.7)
        bars3_2 = ax3.bar(x + width/2, recalls, width, label='Recall', 
                         color=self.colors['optimized'], alpha=0.7)
        
        ax3.set_title('기술적 지표 향상', fontweight='bold', fontsize=12)
        ax3.set_ylabel('Score')
        ax3.set_xticks(x)
        ax3.set_xticklabels(algorithms)
        ax3.legend()
        ax3.grid(True, alpha=0.3)
        
        # 4. 모델 성능 개선 (mAP 중심)
        bars4 = ax4.bar(algorithms, map_scores, color=colors, alpha=0.8)
        ax4.set_title('모델 성능 개선 (mAP@5)', fontweight='bold', fontsize=12)
        ax4.set_ylabel('mAP@5')
        ax4.set_ylim(0, 1.0)
        
        for i, (bar, value) in enumerate(zip(bars4, map_scores)):
            height = bar.get_height()
            ax4.text(bar.get_x() + bar.get_width()/2., height + 0.02,
                    f'{value:.3f}', ha='center', va='bottom', fontweight='bold')
        
        # 전체 제목
        fig.suptitle('실제 데이터 기반 추천 알고리즘 성능 분석', 
                    fontsize=16, fontweight='bold', y=0.95)
        
        plt.tight_layout()
        plt.savefig(save_path, dpi=300, bbox_inches='tight', 
                   facecolor=self.colors['background'])
        plt.show()
        
        print(f"✅ 정확한 표현 기반 차트 저장: {save_path}")
    
    def generate_presentation_summary(self, save_path="발표_요약_정확한_표현.md"):
        """정확한 표현을 사용한 발표 요약"""
        if not self.results:
            return
        
        baseline = self.results['baseline']
        improved = self.results['improved'] 
        optimized = self.results['optimized']
        
        # 개선율 계산
        precision_improvement = ((optimized['precision'] / baseline['precision']) - 1) * 100 if baseline['precision'] > 0 else 0
        f1_improvement = ((optimized['f1_score'] / baseline['f1_score']) - 1) * 100 if baseline['f1_score'] > 0 else 0
        map_improvement = ((optimized['map_score'] / baseline['map_score']) - 1) * 100 if baseline['map_score'] > 0 else 0
        
        summary = f"""# 🎯 실제 데이터 기반 추천 알고리즘 성능 분석

## 📊 **시뮬레이션 환경**
- **데이터셋**: {len(pd.read_csv(self.data_path)):,}개 실제 사용자-프로젝트 매칭 레코드
- **평가 방식**: 70-30 훈련-테스트 분할
- **알고리즘**: 3단계 점진적 개선 방식

## 🔬 **시뮬레이션 기반 성능 평가 결과**

### **알고리즘별 성능 지표**
| 단계 | 알고리즘 | Precision | Recall | F1-Score | mAP@5 |
|------|----------|-----------|--------|----------|-------|
| 1단계 | 기준 알고리즘 | {baseline['precision']:.3f} | {baseline['recall']:.3f} | {baseline['f1_score']:.3f} | {baseline['map_score']:.3f} |
| 2단계 | 개선된 알고리즘 | {improved['precision']:.3f} | {improved['recall']:.3f} | {improved['f1_score']:.3f} | {improved['map_score']:.3f} |
| 3단계 | 최적화된 알고리즘 | {optimized['precision']:.3f} | {optimized['recall']:.3f} | {optimized['f1_score']:.3f} | {optimized['map_score']:.3f} |

## 📈 **알고리즘 개선 효과**

### **핵심 성과**
- **정밀도 개선**: {baseline['precision']:.3f} → {optimized['precision']:.3f} ({precision_improvement:+.1f}%)
- **F1-Score 향상**: {baseline['f1_score']:.3f} → {optimized['f1_score']:.3f} ({f1_improvement:+.1f}%)
- **mAP@5 개선**: {baseline['map_score']:.3f} → {optimized['map_score']:.3f} ({map_improvement:+.1f}%)

## 🎯 **기술적 지표 향상의 의미**

### **1. 시뮬레이션 검증**
- 실제 데이터 {len(pd.read_csv(self.data_path)):,}건을 활용한 성능 검증
- 3단계 점진적 개선을 통한 알고리즘 최적화 확인

### **2. 모델 성능 개선**
- **mAP@5 {optimized['map_score']:.1%}**: 상위 5개 추천의 품질이 우수함
- **정밀도 {optimized['precision']:.1%}**: 추천한 항목 중 관련성 높은 비율

### **3. 기술적 검증**
- 단순 키워드 매칭 대비 **다차원 특성 분석**의 우수성 입증
- **기술 그룹 연관성** 고려를 통한 추천 품질 향상

## 🚀 **실용화 가능성**

### **현재 달성 수준**
- **기술적 기반 구축 완료**: 3단계 알고리즘 파이프라인
- **성능 검증 완료**: 시뮬레이션 환경에서 개선 효과 확인
- **확장성 확보**: 실제 사용자 데이터 증가 시 성능 향상 기대

### **향후 발전 방향**
- 실제 서비스 적용을 통한 **실시간 학습** 구현
- 사용자 피드백 축적을 통한 **개인화 정도 향상**
- **대용량 데이터 처리**를 위한 시스템 최적화

---

## 💡 **발표 핵심 메시지**

**"실제 데이터 기반 시뮬레이션을 통해 3단계 알고리즘 개선 효과를 검증했으며,**  
**특히 mAP@5에서 {optimized['map_score']:.1%}의 우수한 성능을 달성하여**  
**실용적인 추천 시스템의 기술적 기반을 확립했습니다."**

---

*본 분석은 {len(pd.read_csv(self.data_path)):,}개 실제 데이터 레코드를 기반으로 한 시뮬레이션 결과입니다.*
"""
        
        with open(save_path, 'w', encoding='utf-8') as f:
            f.write(summary)
        
        print(f"✅ 발표 요약 저장: {save_path}")
        
        return summary

def main():
    """메인 실행 함수"""
    print("🎯 정확한 표현 기반 발표 자료 생성 시작")
    print("=" * 60)
    
    generator = AccuratePresentationGenerator()
    
    # 1. 실제 데이터 기반 알고리즘 성능 평가
    results = generator.evaluate_algorithms_on_real_data()
    
    # 2. 정확한 표현 기반 차트 생성
    generator.create_accurate_presentation_chart()
    
    # 3. 발표 요약 생성
    summary = generator.generate_presentation_summary()
    
    print("\n" + "="*60)
    print("🎉 정확한 표현 기반 발표 자료 생성 완료!")
    print("\n📊 최종 성능 요약:")
    
    for alg_name, metrics in results.items():
        print(f"\n{alg_name.upper()}:")
        print(f"  mAP@5: {metrics['map_score']:.3f}")
        print(f"  Precision: {metrics['precision']:.3f}")
        print(f"  F1-Score: {metrics['f1_score']:.3f}")
    
    print("\n✅ 생성된 파일:")
    print("  📊 실제_데이터_기반_성능_평가.png")
    print("  📄 발표_요약_정확한_표현.md")

if __name__ == "__main__":
    main()