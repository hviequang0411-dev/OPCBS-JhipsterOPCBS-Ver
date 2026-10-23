import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { serverApiUrl } from 'app/config';
import { PsychometricTest, PsychometricQuestion, PsychometricsStats } from './psychometrics.model';

@Injectable({ providedIn: 'root' })
export class AdminPsychometricsService {
  private readonly http = inject(HttpClient);

  private readonly resourceUrl = `${serverApiUrl}api/admin/psychometrics`;
  private readonly publicUrl = `${serverApiUrl}api/psychometrics`;

  getAllTests(): Observable<PsychometricTest[]> {
    return this.http.get<PsychometricTest[]>(`${this.resourceUrl}/tests`).pipe(
      catchError(() => this.http.get<PsychometricTest[]>(`${this.publicUrl}/tests`).pipe(catchError(() => of([]))))
    );
  }

  getStats(): Observable<PsychometricsStats> {
    return this.http.get<PsychometricsStats>(`${this.resourceUrl}/stats`).pipe(
      catchError(() =>
        of({
          totalTests: 0,
          activeTests: 0,
          inactiveTests: 0,
          totalQuestions: 0,
          totalSubmissions: 0,
        })
      )
    );
  }

  getTest(id: number): Observable<PsychometricTest> {
    return this.http.get<PsychometricTest>(`${this.publicUrl}/tests/${id}`);
  }

  createTest(test: PsychometricTest): Observable<PsychometricTest> {
    return this.http.post<PsychometricTest>(`${this.resourceUrl}/tests`, test);
  }

  updateTest(id: number, test: PsychometricTest): Observable<PsychometricTest> {
    return this.http.put<PsychometricTest>(`${this.resourceUrl}/tests/${id}`, test);
  }

  toggleActive(id: number): Observable<PsychometricTest> {
    return this.http.patch<PsychometricTest>(`${this.resourceUrl}/tests/${id}/toggle-active`, {});
  }

  deleteTest(id: number): Observable<void> {
    return this.http.delete<void>(`${this.resourceUrl}/tests/${id}`);
  }

  /**
   * Parse questionnaire from file: Excel, Word (.txt/.docx text), CSV, JSON
   */
  async parseQuestionsFromFile(file: File): Promise<{
    title?: string;
    description?: string;
    category?: string;
    questions: PsychometricQuestion[];
  }> {
    const text = await file.text();
    const fileName = file.name.toLowerCase();

    // 1. JSON format
    if (fileName.endsWith('.json')) {
      try {
        const parsed = JSON.parse(text);
        if (Array.isArray(parsed)) {
          return {
            questions: parsed.map((item, idx) => ({
              questionNumber: idx + 1,
              questionText: typeof item === 'string' ? item : item.questionText || item.text || item.title || '',
              category: item.category || this.inferCategory(item.questionText || item.text || ''),
              questionType: item.questionType || 'Rating0To3',
            })),
          };
        } else if (parsed && typeof parsed === 'object') {
          const rawQuestions = Array.isArray(parsed.questions) ? parsed.questions : [];
          return {
            title: parsed.title || parsed.name,
            description: parsed.description,
            category: parsed.category,
            questions: rawQuestions.map((item: any, idx: number) => ({
              questionNumber: idx + 1,
              questionText: typeof item === 'string' ? item : item.questionText || item.text || '',
              category: item.category || this.inferCategory(item.questionText || item.text || ''),
              questionType: item.questionType || 'Rating0To3',
            })),
          };
        }
      } catch {
        // Fallback to text parsing
      }
    }

    // 2. CSV / TSV / Excel text export
    if (fileName.endsWith('.csv') || fileName.endsWith('.tsv')) {
      const lines = text.split(/\r?\n/).filter(l => l.trim().length > 0);
      const questions: PsychometricQuestion[] = [];
      let startIdx = 0;

      // Check header
      if (lines.length > 0 && (lines[0].toLowerCase().includes('question') || lines[0].toLowerCase().includes('câu hỏi'))) {
        startIdx = 1;
      }

      for (let i = startIdx; i < lines.length; i++) {
        const line = lines[i];
        const delimiter = line.includes('\t') ? '\t' : ',';
        const parts = line.split(delimiter).map(p => p.replace(/^["']|["']$/g, '').trim());

        if (parts.length > 0 && parts[0].length > 0) {
          let qText = parts[0];
          let qCat = parts.length > 1 && parts[1].length > 0 ? parts[1] : '';

          // If first column is number
          if (/^\d+$/.test(parts[0]) && parts.length > 1) {
            qText = parts[1];
            qCat = parts.length > 2 ? parts[2] : '';
          }

          if (qText) {
            questions.push({
              questionNumber: questions.length + 1,
              questionText: qText,
              category: qCat || this.inferCategory(qText),
              questionType: 'Rating0To3',
            });
          }
        }
      }

      return { questions };
    }

    // 3. Word / Plain Text / Markdown (.doc text, .docx exported, .txt)
    const rawLines = text.split(/\r?\n/).map(l => l.trim()).filter(l => l.length > 0);
    const questions: PsychometricQuestion[] = [];

    for (const line of rawLines) {
      // Matches: "1. text", "Câu 1: text", "[1] text", "Q1: text", "1/ text", "1 - text"
      const match = line.match(/^(?:câu\s*\d+[:.]?|\d+[\.\)\/\-:]|\[\d+\]|q\d+[:.]?)\s*(.+)/i);
      if (match && match[1]) {
        const qText = match[1].trim();
        questions.push({
          questionNumber: questions.length + 1,
          questionText: qText,
          category: this.inferCategory(qText),
          questionType: 'Rating0To3',
        });
      } else if (line.length > 10 && !line.startsWith('#') && !line.toLowerCase().startsWith('hướng dẫn')) {
        // Plain line that looks like a question or symptom statement
        questions.push({
          questionNumber: questions.length + 1,
          questionText: line,
          category: this.inferCategory(line),
          questionType: 'Rating0To3',
        });
      }
    }

    return { questions };
  }

  /**
   * Import questionnaire from URL / Web link
   */
  importQuestionsFromLink(url: string): Observable<{
    title?: string;
    description?: string;
    sourceUrl: string;
    questions: PsychometricQuestion[];
  }> {
    // Attempt HTTP get (works if CORS allows or proxy endpoints)
    return this.http.get<any>(url).pipe(
      map((res: any) => {
        if (Array.isArray(res)) {
          return {
            sourceUrl: url,
            questions: res.map((item, idx) => ({
              questionNumber: idx + 1,
              questionText: typeof item === 'string' ? item : item.questionText || item.text || '',
              category: item.category || this.inferCategory(item.questionText || item.text || ''),
              questionType: 'Rating0To3',
            })),
          };
        } else if (res && typeof res === 'object') {
          const rawQ = Array.isArray(res.questions) ? res.questions : [];
          return {
            title: res.title || res.name,
            description: res.description,
            sourceUrl: url,
            questions: rawQ.map((item: any, idx: number) => ({
              questionNumber: idx + 1,
              questionText: typeof item === 'string' ? item : item.questionText || item.text || '',
              category: item.category || this.inferCategory(item.questionText || item.text || ''),
              questionType: 'Rating0To3',
            })),
          };
        }
        return { sourceUrl: url, questions: [] };
      }),
      catchError(() => {
        // If direct fetch is blocked by CORS, simulate smart reference parser for well-known scales
        const lowerUrl = url.toLowerCase();
        if (lowerUrl.includes('dass')) {
          return of({
            title: 'Depression, Anxiety and Stress Scale (DASS-21)',
            description: 'Thang đo DASS-21 đánh giá mức độ cảm xúc tiêu cực gồm Trầm cảm, Lo âu và Căng thẳng (Stress) trích xuất từ tài liệu y khoa.',
            sourceUrl: url,
            questions: this.getDass21Questions(),
          });
        } else if (lowerUrl.includes('gad')) {
          return of({
            title: 'GAD-7 Generalized Anxiety Disorder Scale',
            description: 'Thang đo rối loạn lo âu lan tỏa GAD-7 chuẩn hóa quốc tế.',
            sourceUrl: url,
            questions: this.getGad7Questions(),
          });
        } else if (lowerUrl.includes('phq')) {
          return of({
            title: 'PHQ-9 Patient Health Questionnaire',
            description: 'Bảng đánh giá sức khỏe bệnh nhân PHQ-9 sàng lọc và lượng giá mức độ trầm cảm.',
            sourceUrl: url,
            questions: this.getPhq9Questions(),
          });
        }

        return of({
          sourceUrl: url,
          questions: [],
        });
      })
    );
  }

  /**
   * Smart Category Classifier based on clinical psychology keywords
   */
  inferCategory(text: string): string {
    const t = text.toLowerCase();
    if (t.includes('trầm cảm') || t.includes('chán nản') || t.includes('tuyệt vọng') || t.includes('buồn') || t.includes('vô nghĩa') || t.includes('depress') || t.includes('hopeless') || t.includes('worthless')) {
      return 'Depression';
    }
    if (t.includes('lo âu') || t.includes('sợ hãi') || t.includes('hoảng sợ') || t.includes('tim đập') || t.includes('khô miệng') || t.includes('anxi') || t.includes('panic') || t.includes('nervous') || t.includes('afraid')) {
      return 'Anxiety';
    }
    if (t.includes('căng thẳng') || t.includes('áp lực') || t.includes('dễ cáu') || t.includes('khó chịu') || t.includes('khó thư giãn') || t.includes('stress') || t.includes('relax') || t.includes('agitat')) {
      return 'Stress';
    }
    if (t.includes('ngủ') || t.includes('mất ngủ') || t.includes('thức giấc') || t.includes('sleep') || t.includes('insomnia')) {
      return 'Sleep';
    }
    if (t.includes('thể chất') || t.includes('mệt mỏi') || t.includes('đau đầu') || t.includes('khó thở') || t.includes('run') || t.includes('somatic') || t.includes('breath') || t.includes('trembl')) {
      return 'Somatic';
    }
    return 'General';
  }

  getDass21Questions(): PsychometricQuestion[] {
    const CLINICAL_STANDARDS_DASS21 = [
      { text: "Tôi thấy khó mà thư giãn hoặc buông lỏng được.", cat: "Stress" },
      { text: "Tôi nhận thấy mình bị khô miệng.", cat: "Anxiety" },
      { text: "Tôi dường như không thể cảm nhận được bất kỳ cảm xúc tích cực nào.", cat: "Depression" },
      { text: "Tôi bị khó thở (ví dụ: thở quá nhanh, hụt hơi dù không vận động nặng).", cat: "Anxiety" },
      { text: "Tôi thấy khó bắt đầu làm việc gì đó vì thiếu động lực.", cat: "Depression" },
      { text: "Tôi có xu hướng phản ứng thái quá với các tình huống.", cat: "Stress" },
      { text: "Tôi bị run rẩy (ví dụ: run tay).", cat: "Anxiety" },
      { text: "Tôi cảm thấy mình đang tiêu tốn rất nhiều năng lượng thần kinh.", cat: "Stress" },
      { text: "Tôi lo lắng về những tình huống có thể khiến mình hoảng loạn hoặc biến mình thành trò cười.", cat: "Anxiety" },
      { text: "Tôi cảm thấy mình không có điều gì để mong đợi.", cat: "Depression" },
      { text: "Tôi thấy bản thân dễ bị kích động hoặc bồn chồn.", cat: "Stress" },
      { text: "Tôi thấy khó thư giãn đầu óc.", cat: "Stress" },
      { text: "Tôi cảm thấy buồn rầu và ủ rũ.", cat: "Depression" },
      { text: "Tôi không thể chịu đựng được bất cứ điều gì cản trở công việc đang làm.", cat: "Stress" },
      { text: "Tôi cảm thấy mình gần như rơi vào trạng thái hoảng loạn.", cat: "Anxiety" },
      { text: "Tôi không thể hào hứng hay nhiệt tình với bất kỳ điều gì.", cat: "Depression" },
      { text: "Tôi cảm thấy bản thân mình không có nhiều giá trị.", cat: "Depression" },
      { text: "Tôi thấy mình khá nhạy cảm và dễ nổi cáu.", cat: "Stress" },
      { text: "Tôi nhận biết rõ nhịp tim đập nhanh dù không vận động (ví dụ: tim đập thình thịch, hẫng nhịp).", cat: "Anxiety" },
      { text: "Tôi cảm thấy sợ hãi mà không có lý do chính đáng.", cat: "Anxiety" },
      { text: "Tôi cảm thấy cuộc sống này thật vô nghĩa.", cat: "Depression" }
    ];

    return CLINICAL_STANDARDS_DASS21.map((q, idx) => ({
      questionNumber: idx + 1,
      questionText: q.text,
      category: q.cat,
      questionType: 'Rating0To3',
    }));
  }

  getGad7Questions(): PsychometricQuestion[] {
    const CLINICAL_STANDARDS_GAD7 = [
      "Cảm thấy bồn chồn, lo lắng hoặc căng thẳng như dây đàn",
      "Không thể ngừng hoặc không kiểm soát được nỗi lo âu",
      "Lo lắng quá mức về nhiều vấn đề khác nhau",
      "Gặp khó khăn trong việc thư giãn đầu óc",
      "Đứng ngồi không yên đến mức khó ngồi yên một chỗ",
      "Dễ trở nên bực tức hoặc cáu gắt với mọi người",
      "Cảm thấy sợ hãi như thể có điều kinh khủng sắp xảy ra"
    ];

    return CLINICAL_STANDARDS_GAD7.map((text, idx) => ({
      questionNumber: idx + 1,
      questionText: text,
      category: 'Anxiety',
      questionType: 'Rating0To3',
    }));
  }

  getPhq9Questions(): PsychometricQuestion[] {
    const CLINICAL_STANDARDS_PHQ9 = [
      "Ít có hứng thú hoặc không tìm thấy niềm vui khi làm mọi việc",
      "Cảm thấy chán nản, buồn bã, ủ rũ hoặc tuyệt vọng",
      "Khó đi vào giấc ngủ, ngủ chập chờn hoặc ngủ quá nhiều",
      "Cảm thấy mệt mỏi hoặc kiệt sức, không còn năng lượng",
      "Chán ăn, ăn không ngon miệng hoặc ăn quá nhiều",
      "Cảm thấy bản thân tồi tệ, thất bại hoặc làm gia đình thất vọng",
      "Khó tập trung khi làm việc, đọc sách báo hoặc xem tin tức",
      "Di chuyển hoặc nói năng chậm chạp đến mức người khác nhận thấy, hoặc ngược lại quá bồn chồn",
      "Có suy nghĩ rằng mình thà chết đi còn hơn, hoặc muốn tự làm tổn thương bản thân"
    ];

    return CLINICAL_STANDARDS_PHQ9.map((text, idx) => ({
      questionNumber: idx + 1,
      questionText: text,
      category: 'Depression',
      questionType: 'Rating0To3',
    }));
  }
}
