export interface PsychometricQuestion {
  id?: number;
  questionText: string;
  questionNumber: number;
  category?: string; // 'General' | 'Depression' | 'Anxiety' | 'Stress' | 'Sleep' | 'Somatic'
  questionType?: string; // 'Rating0To3' | 'Rating1To5' | 'MultipleChoice' | 'YesNo'
  optionsJson?: string;
}

export interface PsychometricTest {
  id?: number;
  title: string;
  description?: string;
  testType: string; // 'DASS21' | 'PHQ9' | 'GAD7' | 'CUSTOM'
  category?: string; // 'Depression' | 'Anxiety' | 'Stress' | 'General Wellbeing'
  purpose?: string;
  scoreRangesJson?: string;
  sourceUrl?: string;
  isActive?: boolean;
  questions?: PsychometricQuestion[];
}

export interface PsychometricsStats {
  totalTests: number;
  activeTests: number;
  inactiveTests: number;
  totalQuestions: number;
  totalSubmissions: number;
}
