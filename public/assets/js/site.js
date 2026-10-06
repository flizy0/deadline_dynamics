"use strict";

(() => {
  const study = window.STUDY_DATA;
  const copy = {
    ru: {
      "common.skip":"К основному содержанию","common.navigation":"Основная навигация","common.language":"Язык","common.open":"Открыть меню","common.close":"Закрыть меню","common.noData":"Нет данных","common.category":"Категория","common.responses":"Ответы","common.total":"Всего","common.known":"Известные исходы","common.onTime":"Вовремя","common.late":"Позже срока","common.pending":"Пока не сдано","common.percent":"Процент","common.n":"n",
      "brand.title":"До дедлайна","brand.course":"Теория вероятностей и статистика","nav.research":"ИССЛЕДОВАНИЕ","nav.overview":"Обзор","nav.data":"Данные","nav.lab":"Лаборатория вероятностей","nav.method":"Формулы и методы","nav.tagline":"Учебное исследование о сроках выполнения заданий.","footer":"Учебный проект по теории вероятностей и статистике","footer.method":"Как читать методы",
      "overview.title":"Обзор","overview.eyebrow":"ИССЛЕДОВАНИЕ СТУДЕНЧЕСКИХ ДЕДЛАЙНОВ","overview.subtitle":"Мы изучаем, связана ли своевременная сдача задания с тем, когда студент начинает над ним работать.","overview.survey":"Открыть анкету","overview.responses":"Ответы на анкету","overview.eligible":"Подходящие наблюдения","overview.known":"Известен исход сдачи","overview.pending":"Пока не сдано","overview.findingEyebrow":"ОСНОВНОЙ РЕЗУЛЬТАТ ВЫБОРКИ","overview.findingTitle":"Сколько известных исходов завершились сдачей в срок?","overview.findingText":"В этой выборке 17 из 23 заданий с известным исходом были сданы не позже первоначального дедлайна.","overview.pendingNote":"Ещё 6 подходящих ответов относятся к заданиям, которые на момент ответа пока не были сданы; они не входят в знаменатель 23.","overview.patternEyebrow":"ВРЕМЯ НАЧАЛА И ИСХОД","overview.patternTitle":"Как распределились исходы по времени начала?","overview.knownDenominator":"Доля вовремя считается среди известных исходов","overview.sourceTitle":"Источник данных","overview.sourceText":"Добровольная анкета Google Forms, ответы собраны с 30 сентября по 2 октября 2026 года.","overview.limit":"Выборка добровольная и неслучайная. Наблюдаемая связь не доказывает причинность и не описывает всех студентов.",
      "view.percent":"Проценты","view.count":"Количество","view.table":"Таблица","view.theoretical":"Теория","view.empirical":"Симуляция","view.ontime":"Вовремя","view.late":"Позже срока","view.pending":"Пока не сдано","view.axisPercent":"Доля среди известных исходов, %","view.axisCount":"Количество ответов","view.axisProbability":"Вероятность, %","view.axisRuns":"Число повторений","view.axisEstimate":"Оценка вероятности, %","view.threshold":"Порог события",
      "data.title":"Что показывают ответы?","data.eyebrow":"ОПИСАНИЕ ВЫБОРКИ","data.subtitle":"Частоты и сравнения помогают увидеть, как распределены ответы и насколько различаются исходы по группам.","data.denominator":"Распределения включают все 29 подходящих ответов. Доля «вовремя» рассчитывается только среди известных исходов; «пока не сдано» показано отдельно.","data.distributionEyebrow":"ОДНА ПЕРЕМЕННАЯ","data.distributionTitle":"Как часто встречается каждый ответ?","data.chooseVariable":"Переменная","data.comparisonEyebrow":"СВЯЗЬ С ИСХОДОМ","data.comparisonTitle":"Как распределились исходы внутри групп?","data.choosePredictor":"Группирующая переменная","data.knownOnly":"Проценты «вовремя» и «позже» рассчитаны среди известных исходов в каждой группе. Ожидающие сдачи остаются отдельным количеством.","data.exact":"Точные значения","data.snapshotTitle":"Срез данных","data.snapshotText":"52 ответа получены через Google Forms с 30 сентября по 2 октября 2026 года. Публикуются агрегированные частоты, а не строки индивидуальных ответов.","data.total":"Всего","data.share":"Доля от всех подходящих ответов","data.knownN":"Известно n","data.onTimePercent":"Вовремя среди известных","data.relationshipAccessible":"Таблица показывает абсолютные значения и знаменатель для каждой категории.","data.variable.allotted":"Сколько времени дали на задание","data.variable.start":"Когда начали работать","data.variable.outcome":"Исход сдачи относительно срока","data.variable.extension":"Переносили ли срок","data.variable.planning":"Планирование работы","data.variable.difficulty":"Оценка сложности","data.variable.otherDeadlines":"Другие задания на той же неделе",
      "cat.sameDay":"В день дедлайна","cat.oneDay":"За 1 день","cat.twoDays":"За 2 дня","cat.threeFourDays":"За 3–4 дня","cat.fiveSevenDays":"За 5–7 дней","cat.eightPlusDays":"За 8 и более дней","cat.dontRemember":"Не помню","cat.dueDate":"В день дедлайна, до установленного времени","cat.afterDeadline":"После первоначального дедлайна","cat.notStarted":"Ещё не начал(а)","cat.onTime":"Не позже первоначального дедлайна","cat.late":"Позже первоначального дедлайна","cat.pending":"Пока не сдал(а)","cat.no":"Нет","cat.yes":"Да","cat.unknown":"Не знаю или не помню","cat.mentalPlan":"Примерный план в голове","cat.noPlan":"Плана не было","cat.writtenPlan":"Записанный план","cat.veryEasy":"Очень лёгкое","cat.ratherEasy":"Скорее лёгкое","cat.moderate":"Средней сложности","cat.ratherDifficult":"Скорее сложное","cat.veryDifficult":"Очень сложное","cat.none":"Ни одного","cat.one":"Одно","cat.two":"Два","cat.three":"Три","cat.fourPlus":"Четыре или больше",
      "lab.title":"Лаборатория вероятностей","lab.eyebrow":"ЭКСПЕРИМЕНТ С БИНОМИАЛЬНОЙ МОДЕЛЬЮ","lab.subtitle":"Если вероятность сдачи в срок равна p, сколько студентов из группы n могут успеть?","lab.individual":"Один студент — одно испытание","lab.group":"X — число сдач в срок в группе","lab.explanation":"Успех означает сдачу не позже первоначального дедлайна. Для модели каждому студенту задаётся одинаковая вероятность p, а результаты считаются независимыми.","lab.setupEyebrow":"НАСТРОЙКИ ЭКСПЕРИМЕНТА","lab.setupTitle":"Выберите вероятность и размер группы","lab.probabilitySource":"Источник вероятности p","lab.observed":"Наблюдаемая в анкете","lab.manual":"Задать вручную","lab.groupSize":"Размер группы n","lab.repetitions":"Повторений эксперимента","lab.manualProbability":"Вероятность успеха p","lab.threshold":"Интересующее событие","lab.atLeast":"Не менее","lab.onTime":"сдадут в срок","lab.run":"Запустить эксперимент","lab.observedOrigin":"Наблюдаемая доля: 17 из 23 известных исходов; 6 ожидающих ответов не входят в расчёт.","lab.manualOrigin":"Ручное значение p не вычисляется из ответов анкеты.","lab.advanced":"Дополнительные настройки","lab.seed":"Начальное значение генератора (необязательно)","lab.seedHelp":"Одинаковые параметры и одно значение seed воспроизводят тот же результат.","lab.resultEyebrow":"РЕЗУЛЬТАТ ЭКСПЕРИМЕНТА","lab.resultTitle":"Что может произойти в группе?","lab.probability":"Вероятность успеха","lab.resultGroup":"Размер одной группы","lab.expected":"Ожидаемое число сдач в срок","lab.viewDistribution":"Распределение","lab.viewTail":"Хвостовая вероятность","lab.viewConvergence":"Сходимость","lab.viewGroup":"Одна группа","lab.oneGroupTitle":"Одна случайная группа","lab.oneGroupDescription":"Каждый знак — один смоделированный студент.","lab.anotherGroup":"Смоделировать ещё одну группу","lab.exact":"Точные значения и сравнение с теорией","lab.assumptionsTitle":"Что предполагает модель?","lab.assumption1":"У каждого испытания одна и та же вероятность p.","lab.assumption2":"Результаты студентов считаются независимыми.","lab.assumption3":"Реальное поведение зависит от множества факторов, которые эта простая модель не учитывает.","lab.assumption4":"Наблюдаемое p оценено по выборке; симуляция иллюстрирует вероятность, а не причинный прогноз.","lab.theoryStory":"Чего ожидает теория","lab.simStory":"Что получилось в симуляции","lab.compareStory":"Теория и симуляция","lab.interpretation":"Интерпретация","lab.expectedValue":"Математическое ожидание","lab.average":"Среднее в симуляции","lab.theoreticalTail":"Теоретическая P(X ≥ k)","lab.simulatedTail":"Симуляционная P(X ≥ k)","lab.difference":"Абсолютная разница","lab.interpretClose":"Оценка симуляции близка к теоретическому значению. При большем числе повторений случайная ошибка обычно уменьшается, но не обязана уменьшаться на каждом шаге.","lab.interpretFar":"Разница между оценкой симуляции и теорией — часть случайной вариации. Большее число повторений обычно делает оценку устойчивее, но не гарантирует монотонного сближения.","lab.runs":"групп смоделировано","lab.studentSuccess":"Студент {n}: успех, сдача в срок","lab.studentFailure":"Студент {n}: исход неуспеха в модели","lab.groupOutcome":"{success} из {n} студентов сдали в срок в этой случайной группе.","lab.chartDistribution":"Распределение числа студентов, сдавших в срок","lab.chartTail":"Вероятности исходов; янтарным отмечены значения X ≥ {k}","lab.chartConvergence":"Оценка P(X ≥ {k}) при увеличении числа повторений","lab.chartGroup":"Смоделирована одна группа из {n} студентов.","lab.tableX":"Число сдач в срок X","lab.tableTheory":"Теоретическая вероятность","lab.tableSimulation":"Частота в симуляции","lab.tableThreshold":"В хвосте",
      "method.title":"Формулы и методы","method.eyebrow":"СПРАВОЧНИК ПО МАТЕРИАЛАМ КУРСА","method.subtitle":"Краткая опора по распределениям частот, событиям, условной вероятности, независимости, Байесу и случайным величинам.","method.separate":"Этот раздел — самостоятельный учебный справочник: формулы не подставляют значения из анкеты и не интерпретируют её ответы.","method.navFrequency":"Частоты","method.navEvents":"События","method.navConditional":"Условная вероятность","method.navBayes":"Байес","method.navVariables":"Случайные величины","method.navDistributions":"Распределения","method.frequencyTitle":"Частотные распределения","method.frequencyIntro":"Сначала наблюдения группируют по значениям или интервалам, затем описывают их количеством и долей.","method.absolute":"Абсолютная частота","method.absoluteText":"Сколько раз встретилась категория.","method.relative":"Относительная частота","method.relativeText":"Доля категории от общего числа наблюдений n.","method.percent":"Процентная частота","method.percentText":"Относительная частота, выраженная в процентах.","method.cumulative":"Накопленная частота","method.cumulativeText":"Сумма частот до выбранного значения; имеет смысл для упорядоченных данных.","method.range":"Размах","method.rangeText":"Расстояние между наибольшим и наименьшим числовыми наблюдениями.","method.sturges":"Правило Стерджесса","method.sturgesText":"Приближённое число интервалов при группировке количественных данных.","method.frequencyFootnote":"Для категориальных ответов считают категории напрямую; широкие интервалы нельзя трактовать как точные числовые значения.","method.eventsTitle":"Множества и события","method.eventsIntro":"Случайное событие — подмножество пространства исходов Ω.","method.union":"Объединение","method.unionText":"Происходит A или B, включая случай, когда происходят оба.","method.intersection":"Пересечение","method.intersectionText":"Происходят одновременно и A, и B.","method.complement":"Дополнение","method.complementText":"Вероятность того, что A не произойдёт.","method.addition":"Общее правило сложения","method.additionText":"Пересечение вычитают, чтобы не посчитать общие исходы дважды.","method.disjoint":"Несовместные события","method.disjointText":"Если два события не могут произойти вместе, их вероятности складываются.","method.equiprobable":"Равновозможные исходы","method.equiprobableText":"Число благоприятных исходов делят на число всех исходов, если они равновозможны.","method.conditionalTitle":"Условная вероятность и независимость","method.conditionalIntro":"Условная вероятность уточняет пространство исходов после того, как известно событие B.","method.conditional":"Условная вероятность","method.conditionalText":"Вероятность A при условии, что B уже произошло.","method.multiplication":"Правило умножения","method.multiplicationText":"Связывает совместную вероятность с условной.","method.independent":"Независимые события","method.independentText":"Наступление одного события не меняет вероятность другого.","method.independentCheck":"Проверка независимости","method.independentCheckText":"При P(B) > 0 это равносильно независимости A и B.","method.distinctionTitle":"Не путайте","method.distinction":"Несовместность означает, что A и B не могут произойти вместе. Независимость означает, что одно не меняет вероятность другого. Это разные свойства.","method.bayesTitle":"Формула полной вероятности и Байес","method.bayesIntro":"Когда возможны несколько взаимоисключающих причин A₁,…,Aₖ, результат B складывается из их вкладов.","method.totalProbability":"Формула полной вероятности","method.totalProbabilityText":"A₁,…,Aₖ образуют полную группу событий.","method.bayes":"Формула Байеса","method.bayesText":"Обновляет вероятность причины Aⱼ после наблюдения результата B.","method.variablesTitle":"Дискретные случайные величины","method.variablesIntro":"Случайная величина X сопоставляет каждому исходу числовое значение и имеет распределение вероятностей.","method.pmf":"Функция вероятностей","method.pmfText":"Вероятности возможных значений неотрицательны и в сумме равны единице.","method.expectation":"Математическое ожидание","method.expectationText":"Среднее значение в долгосрочном вероятностном смысле.","method.variance":"Дисперсия и стандартное отклонение","method.varianceText":"Описывают разброс значений вокруг математического ожидания.","method.distributionsTitle":"Стандартные распределения","method.distributionsIntro":"Выбор модели зависит от механизма появления случайной величины и предположений задачи.","method.binomial":"Биномиальное распределение","method.binomialText":"Число успехов в n независимых испытаниях с одинаковой вероятностью p.","method.poisson":"Распределение Пуассона","method.poissonText":"Модель числа событий на фиксированном промежутке при параметре интенсивности λ.","method.normal":"Нормальное распределение","method.normalText":"Непрерывное симметричное распределение с центром μ и масштабом σ.","method.zscore":"Стандартизация","method.zscoreText":"Переводит значение в число стандартных отклонений от среднего; для нормальной модели Z имеет N(0,1).","method.sources":"Раздел составлен по лекциям о частотных распределениях, множествах и событиях, условной вероятности, независимых событиях, формуле Байеса и случайных величинах.","method.dataLink":"Перейти к данным исследования"
    },
    kk: {
      "common.skip":"Негізгі мазмұнға өту","common.navigation":"Негізгі мәзір","common.language":"Тіл","common.open":"Мәзірді ашу","common.close":"Мәзірді жабу","common.noData":"Дерек жоқ","common.category":"Санат","common.responses":"Жауап","common.total":"Барлығы","common.known":"Нәтижесі белгілі","common.onTime":"Уақытында","common.late":"Кеш тапсырылды","common.pending":"Әлі тапсырылмаған","common.percent":"Пайыз","common.n":"n",
      "brand.title":"Дедлайнға дейін","brand.course":"Ықтималдықтар теориясы және статистика","nav.research":"ЗЕРТТЕУ","nav.overview":"Шолу","nav.data":"Деректер","nav.lab":"Ықтималдық зертханасы","nav.method":"Формулалар мен әдістер","nav.tagline":"Тапсырманы орындау мерзімі туралы оқу-зерттеу жұмысы.","footer":"Ықтималдықтар теориясы мен статистика пәні бойынша оқу жобасы","footer.method":"Әдістерді оқу",
      "overview.title":"Шолу","overview.eyebrow":"СТУДЕНТТЕРДІҢ ДЕДЛАЙНДАРЫ ТУРАЛЫ ЗЕРТТЕУ","overview.subtitle":"Тапсырманы уақытында тапсыру мен оны орындауды қашан бастаудың арасында байланыс бар-жоғын зерттейміз.","overview.survey":"Сауалнамаға өту","overview.responses":"Сауалнама жауаптары","overview.eligible":"Талдауға жарамды бақылау","overview.known":"Тапсыру нәтижесі белгілі","overview.pending":"Әлі тапсырылмаған","overview.findingEyebrow":"ІРІКТЕМЕНІҢ НЕГІЗГІ НӘТИЖЕСІ","overview.findingTitle":"Нәтижесі белгілі тапсырмалардың қаншасы уақытында тапсырылды?","overview.findingText":"Осы іріктемеде нәтижесі белгілі 23 тапсырманың 17-сі бастапқы дедлайннан кешіктірілмей тапсырылған.","overview.pendingNote":"Тағы 6 жарамды жауап берілген кезде тапсырма әлі тапсырылмаған. Бұл жауаптар 23 деген бөлімге қосылмайды.","overview.patternEyebrow":"ЖҰМЫСТЫ БАСТАУ УАҚЫТЫ ЖӘНЕ НӘТИЖЕСІ","overview.patternTitle":"Бастау уақытына қарай тапсыру нәтижесі қалай бөлінді?","overview.knownDenominator":"Уақытында тапсыру үлесі нәтижесі белгілі жауаптар бойынша есептеледі","overview.sourceTitle":"Дереккөз","overview.sourceText":"Google Forms арқылы жиналған ерікті сауалнама жауаптары, 2026 жылғы 30 қыркүйек пен 2 қазан аралығы.","overview.limit":"Іріктеме ерікті әрі кездейсоқ емес. Байқалған байланыс себеп-салдарды дәлелдемейді және барлық студентті сипаттамайды.",
      "view.percent":"Пайыз","view.count":"Саны","view.table":"Кесте","view.theoretical":"Теориялық","view.empirical":"Симуляция","view.ontime":"Уақытында","view.late":"Кеш","view.pending":"Әлі тапсырылмаған","view.axisPercent":"Белгілі нәтижелер ішіндегі үлес, %","view.axisCount":"Жауап саны","view.axisProbability":"Ықтималдық, %","view.axisRuns":"Қайталау саны","view.axisEstimate":"Ықтималдық бағасы, %","view.threshold":"Оқиға шегі",
      "data.title":"Жауаптар нені көрсетеді?","data.eyebrow":"ІРІКТЕМЕНІ СИПАТТАУ","data.subtitle":"Жиіліктер жауаптардың қалай бөлінгенін, ал салыстырулар топтардағы нәтижелердің қаншалықты ерекшеленетінін көрсетеді.","data.denominator":"Бөліністерге талдауға жарамды 29 жауаптың бәрі кіреді. «Уақытында» үлесі тек нәтижесі белгілі тапсырмалармен есептеледі; әлі тапсырылмағаны бөлек көрсетіледі.","data.distributionEyebrow":"БІР АЙНЫМАЛЫ","data.distributionTitle":"Әр жауап қаншалықты жиі кездеседі?","data.chooseVariable":"Айнымалы","data.comparisonEyebrow":"ТАПСЫРУ НӘТИЖЕСІМЕН БАЙЛАНЫС","data.comparisonTitle":"Әр топта тапсыру нәтижелері қалай бөлінді?","data.choosePredictor":"Топтастыру айнымалысы","data.knownOnly":"«Уақытында» және «кеш» пайызы әр топтағы нәтижесі белгілі тапсырмалар бойынша есептеледі. Күтіп тұрған жауаптар бөлек саналады.","data.exact":"Нақты мәндер","data.snapshotTitle":"Дерек жиынтығы","data.snapshotText":"Google Forms арқылы 2026 жылғы 30 қыркүйек пен 2 қазан аралығында 52 жауап жиналды. Жеке жауаптар емес, жиынтық жиіліктер жарияланады.","data.total":"Барлығы","data.share":"Барлық жарамды жауаптардағы үлесі","data.knownN":"Белгілі n","data.onTimePercent":"Белгілі нәтижелердің ішіндегі уақытында тапсырылғаны","data.relationshipAccessible":"Кестеде әр санаттың нақты мәндері мен бөлім саны берілген.","data.variable.allotted":"Тапсырманы орындауға берілген уақыт","data.variable.start":"Жұмысты бастау уақыты","data.variable.outcome":"Дедлайнға қатысты тапсыру нәтижесі","data.variable.extension":"Мерзім ұзартылды ма","data.variable.planning":"Жұмысты жоспарлау","data.variable.difficulty":"Қиындық бағасы","data.variable.otherDeadlines":"Сол аптадағы басқа тапсырмалар",
      "cat.sameDay":"Дедлайн күні","cat.oneDay":"1 күн бұрын","cat.twoDays":"2 күн бұрын","cat.threeFourDays":"3–4 күн бұрын","cat.fiveSevenDays":"5–7 күн бұрын","cat.eightPlusDays":"8 немесе одан көп күн бұрын","cat.dontRemember":"Есімде жоқ","cat.dueDate":"Дедлайн күні, белгіленген уақытқа дейін","cat.afterDeadline":"Бастапқы дедлайннан кейін","cat.notStarted":"Әлі бастаған жоқпын","cat.onTime":"Бастапқы дедлайнға дейін немесе сол уақытта","cat.late":"Бастапқы дедлайннан кейін","cat.pending":"Әлі тапсырған жоқпын","cat.no":"Жоқ","cat.yes":"Иә","cat.unknown":"Білмеймін немесе есімде жоқ","cat.mentalPlan":"Ойша шамамен жоспар болды","cat.noPlan":"Жоспар болған жоқ","cat.writtenPlan":"Жазбаша жоспар болды","cat.veryEasy":"Өте жеңіл","cat.ratherEasy":"Біршама жеңіл","cat.moderate":"Орташа қиындықтағы","cat.ratherDifficult":"Біршама күрделі","cat.veryDifficult":"Өте күрделі","cat.none":"Бірде-біреу","cat.one":"Бір","cat.two":"Екі","cat.three":"Үш","cat.fourPlus":"Төрт немесе одан көп",
      "lab.title":"Ықтималдық зертханасы","lab.eyebrow":"БИНОМИАЛДЫ МОДЕЛЬМЕН ЭКСПЕРИМЕНТ","lab.subtitle":"Егер тапсырманы уақытында тапсыру ықтималдығы p болса, n студенттен тұратын топта қаншасы үлгеруі мүмкін?","lab.individual":"Бір студент — бір сынақ","lab.group":"X — топтағы уақытында тапсырылатын жұмыс саны","lab.explanation":"Табыс дегеніміз — бастапқы дедлайннан кешіктірмей тапсыру. Модельде әр студент үшін p ықтималдығы бірдей, ал нәтижелер тәуелсіз деп алынады.","lab.setupEyebrow":"ЭКСПЕРИМЕНТ ПАРАМЕТРЛЕРІ","lab.setupTitle":"Ықтималдық пен топ көлемін таңдаңыз","lab.probabilitySource":"p ықтималдығының көзі","lab.observed":"Сауалнама бойынша байқалғаны","lab.manual":"Қолмен енгізу","lab.groupSize":"Топ көлемі n","lab.repetitions":"Эксперимент қайталануы","lab.manualProbability":"Табыс ықтималдығы p","lab.threshold":"Қызықтыратын оқиға","lab.atLeast":"Кемінде","lab.onTime":"уақытында тапсырады","lab.run":"Экспериментті іске қосу","lab.observedOrigin":"Байқалған үлес: 23 белгілі нәтиженің 17-сі; күтіліп тұрған 6 жауап есепке кірмейді.","lab.manualOrigin":"Қолмен енгізілген p сауалнама жауаптарынан есептелмейді.","lab.advanced":"Қосымша параметрлер","lab.seed":"Кездейсоқ сандар генераторының бастапқы мәні (міндетті емес)","lab.seedHelp":"Бірдей параметрлер мен бірдей seed бір нәтижені қайталайды.","lab.resultEyebrow":"ЭКСПЕРИМЕНТ НӘТИЖЕСІ","lab.resultTitle":"Топта не болуы мүмкін?","lab.probability":"Табыс ықтималдығы","lab.resultGroup":"Бір топтың көлемі","lab.expected":"Уақытында тапсырудың күтілетін саны","lab.viewDistribution":"Үлестірім","lab.viewTail":"Құйрық ықтималдығы","lab.viewConvergence":"Жинақталу","lab.viewGroup":"Бір топ","lab.oneGroupTitle":"Кездейсоқ бір топ","lab.oneGroupDescription":"Әр белгі — модельдегі бір студент.","lab.anotherGroup":"Тағы бір топты модельдеу","lab.exact":"Нақты мәндер мен теориялық салыстыру","lab.assumptionsTitle":"Модель қандай жорамал жасайды?","lab.assumption1":"Әр сынақта p ықтималдығы бірдей.","lab.assumption2":"Студенттердің нәтижелері тәуелсіз деп саналады.","lab.assumption3":"Нақты мінез-құлыққа бұл қарапайым модель ескермейтін көптеген фактор әсер етеді.","lab.assumption4":"Байқалған p іріктемеден бағаланды; симуляция ықтималдықты көрсетеді, себеп-салдар болжамын жасамайды.","lab.theoryStory":"Теория бойынша күтілетіні","lab.simStory":"Симуляция нәтижесі","lab.compareStory":"Теория және симуляция","lab.interpretation":"Түсіндіру","lab.expectedValue":"Математикалық күтім","lab.average":"Симуляциядағы орташа мән","lab.theoreticalTail":"Теориялық P(X ≥ k)","lab.simulatedTail":"Симуляциядағы P(X ≥ k)","lab.difference":"Абсолют айырма","lab.interpretClose":"Симуляция бағасы теориялық мәнге жақын. Қайталау көбейгенде кездейсоқ қате әдетте азаяды, бірақ әр қадам сайын міндетті түрде азаймайды.","lab.interpretFar":"Симуляция бағасы мен теория арасындағы айырма кездейсоқ ауытқудың бір бөлігі. Қайталау санының артуы бағаны тұрақтандыруы мүмкін, бірақ жақындау бірқалыпты болмайды.","lab.runs":"топ модельденді","lab.studentSuccess":"{n}-студент: табыс, уақытында тапсырды","lab.studentFailure":"{n}-студент: модельдегі сәтсіздік","lab.groupOutcome":"Осы кездейсоқ топта {n} студенттің {success}-і уақытында тапсырды.","lab.chartDistribution":"Уақытында тапсырған студенттер санының үлестірімі","lab.chartTail":"Нәтиже ықтималдықтары; X ≥ {k} мәндері сары түспен белгіленген","lab.chartConvergence":"Қайталау саны артқандағы P(X ≥ {k}) бағасы","lab.chartGroup":"{n} студенттен тұратын бір топ модельденді.","lab.tableX":"Уақытында тапсырғандар саны X","lab.tableTheory":"Теориялық ықтималдық","lab.tableSimulation":"Симуляция жиілігі","lab.tableThreshold":"Құйрықта",
      "method.title":"Формулалар мен әдістер","method.eyebrow":"КУРС МАТЕРИАЛДАРЫ БОЙЫНША АНЫҚТАМАЛЫҚ","method.subtitle":"Жиілік үлестірімдері, оқиғалар, шартты ықтималдық, тәуелсіздік, Байес формуласы және кездейсоқ шамалар бойынша қысқаша анықтама.","method.separate":"Бұл бөлім — дербес оқу анықтамалығы: мұнда сауалнама мәндері формулаларға қойылмайды және жауаптар түсіндірілмейді.","method.navFrequency":"Жиіліктер","method.navEvents":"Оқиғалар","method.navConditional":"Шартты ықтималдық","method.navBayes":"Байес","method.navVariables":"Кездейсоқ шамалар","method.navDistributions":"Үлестірімдер","method.frequencyTitle":"Жиілік үлестірімдері","method.frequencyIntro":"Алдымен бақылаулар мәндері немесе интервалдары бойынша топтастырылып, содан кейін саны мен үлесі сипатталады.","method.absolute":"Абсолют жиілік","method.absoluteText":"Санаттың неше рет кездескені.","method.relative":"Салыстырмалы жиілік","method.relativeText":"Санат жиілігінің барлық n бақылауға қатынасы.","method.percent":"Пайыздық жиілік","method.percentText":"Пайызбен көрсетілген салыстырмалы жиілік.","method.cumulative":"Жинақталған жиілік","method.cumulativeText":"Таңдалған мәнге дейінгі жиіліктер қосындысы; реттелген деректерге қолданылады.","method.range":"Өзгеріс ауқымы","method.rangeText":"Ең үлкен және ең кіші сандық бақылаулардың айырмасы.","method.sturges":"Стерджесс ережесі","method.sturgesText":"Сандық деректерді топтастыру кезінде интервал санын жуықтап таңдау.","method.frequencyFootnote":"Санаттық жауаптар санаттар бойынша тікелей саналады; кең интервалдарды нақты сандық мәндер деп қарастыруға болмайды.","method.eventsTitle":"Жиындар мен оқиғалар","method.eventsIntro":"Кездейсоқ оқиға — Ω нәтижелер кеңістігінің ішкі жиыны.","method.union":"Бірігу","method.unionText":"A немесе B оқиғасы болады, екеуі қатар болатын жағдай да кіреді.","method.intersection":"Қиылысу","method.intersectionText":"A мен B оқиғалары қатар болады.","method.complement":"Толықтауыш оқиға","method.complementText":"A оқиғасының болмау ықтималдығы.","method.addition":"Қосудың жалпы ережесі","method.additionText":"Ортақ нәтижелерді екі рет санамау үшін қиылысу ықтималдығын шегереді.","method.disjoint":"Үйлесімсіз оқиғалар","method.disjointText":"Екі оқиға қатар бола алмаса, олардың ықтималдықтары қосылады.","method.equiprobable":"Тең ықтималды нәтижелер","method.equiprobableText":"Нәтижелер тең ықтималды болса, қолайлы нәтижелер саны барлық нәтижелер санына бөлінеді.","method.conditionalTitle":"Шартты ықтималдық және тәуелсіздік","method.conditionalIntro":"B оқиғасы белгілі болғаннан кейін шартты ықтималдық нәтижелер кеңістігін нақтылайды.","method.conditional":"Шартты ықтималдық","method.conditionalText":"B оқиғасы болған жағдайда A оқиғасының ықтималдығы.","method.multiplication":"Көбейту ережесі","method.multiplicationText":"Бірлескен ықтималдықты шартты ықтималдықпен байланыстырады.","method.independent":"Тәуелсіз оқиғалар","method.independentText":"Бір оқиғаның болуы екіншісінің ықтималдығын өзгертпейді.","method.independentCheck":"Тәуелсіздікті тексеру","method.independentCheckText":"P(B) > 0 болса, бұл A мен B тәуелсіз болуына тең.","method.distinctionTitle":"Ажырату керек","method.distinction":"Үйлесімсіздік A мен B қатар бола алмайтынын білдіреді. Тәуелсіздік бір оқиға екіншісінің ықтималдығын өзгертпейтінін білдіреді. Бұлар — бөлек қасиеттер.","method.bayesTitle":"Толық ықтималдық формуласы және Байес теоремасы","method.bayesIntro":"Бірнеше өзара үйлесімсіз A₁,…,Aₖ себеп болуы мүмкін болса, B нәтижесінің ықтималдығы олардың үлестерінен құралады.","method.totalProbability":"Толық ықтималдық формуласы","method.totalProbabilityText":"A₁,…,Aₖ оқиғалары толық топ құрайды.","method.bayes":"Байес формуласы","method.bayesText":"B нәтижесі байқалғаннан кейін Aⱼ себебінің ықтималдығын жаңартады.","method.variablesTitle":"Дискретті кездейсоқ шамалар","method.variablesIntro":"Кездейсоқ X шамасы әр нәтижеге сандық мән сәйкестендіріп, ықтималдық үлестіріміне ие болады.","method.pmf":"Ықтималдық функциясы","method.pmfText":"Мүмкін мәндердің ықтималдықтары теріс емес және қосындысы бірге тең.","method.expectation":"Математикалық күтім","method.expectationText":"Ұзақ мерзімді ықтималдық мағынасындағы орташа мән.","method.variance":"Дисперсия және стандарттық ауытқу","method.varianceText":"Математикалық күтім айналасындағы шашырауды сипаттайды.","method.distributionsTitle":"Негізгі үлестірімдер","method.distributionsIntro":"Модельді таңдау кездейсоқ шаманың пайда болу механизмі мен есептің жорамалдарына байланысты.","method.binomial":"Биномиал үлестірімі","method.binomialText":"Ықтималдығы p бірдей n тәуелсіз сынақтағы табыстар саны.","method.poisson":"Пуассон үлестірімі","method.poissonText":"λ қарқындылық параметрі бар тұрақты аралықтағы оқиғалар санын модельдейді.","method.normal":"Қалыпты үлестірім","method.normalText":"Ортасы μ және масштабы σ болатын үздіксіз симметриялы үлестірім.","method.zscore":"Стандарттау","method.zscoreText":"Мәннің орташа шамадан неше стандарттық ауытқуға алшақ екенін көрсетеді; қалыпты модельде Z ~ N(0,1).","method.sources":"Бөлім жиілік үлестірімдері, жиындар мен оқиғалар, шартты ықтималдық, тәуелсіз оқиғалар, Байес формуласы және кездейсоқ шамалар туралы дәрістерге сүйеніп жасалды.","method.dataLink":"Зерттеу деректеріне өту"
    },
    en: {
      "common.skip":"Skip to main content","common.navigation":"Main navigation","common.language":"Language","common.open":"Open menu","common.close":"Close menu","common.noData":"No data","common.category":"Category","common.responses":"Responses","common.total":"Total","common.known":"Known outcomes","common.onTime":"On time","common.late":"Late","common.pending":"Not submitted yet","common.percent":"Percent","common.n":"n",
      "brand.title":"Before the Deadline","brand.course":"Probability and Statistics","nav.research":"STUDY","nav.overview":"Overview","nav.data":"Data","nav.lab":"Probability Lab","nav.method":"Formulas and Methods","nav.tagline":"A student study of assignment timing and deadlines.","footer":"University project in probability and statistics","footer.method":"How to read the methods",
      "overview.title":"Overview","overview.eyebrow":"STUDENT DEADLINE STUDY","overview.subtitle":"We study whether on-time submission is related to when a student starts working on an assignment.","overview.survey":"Open the survey","overview.responses":"Survey responses","overview.eligible":"Eligible observations","overview.known":"Submission outcome known","overview.pending":"Not submitted yet","overview.findingEyebrow":"MAIN SAMPLE RESULT","overview.findingTitle":"How many known outcomes were submitted on time?","overview.findingText":"In this sample, 17 of 23 assignments with a known outcome were submitted by the original deadline.","overview.pendingNote":"Six other eligible responses were still not submitted when the answer was recorded. They are not part of the denominator 23.","overview.patternEyebrow":"START TIME AND OUTCOME","overview.patternTitle":"How did outcomes vary by start time?","overview.knownDenominator":"On-time share uses known outcomes","overview.sourceTitle":"Data source","overview.sourceText":"Voluntary Google Forms survey. Responses were collected from 30 September to 2 October 2026.","overview.limit":"This is a voluntary, non-random sample. An observed association does not prove causation or describe all students.",
      "view.percent":"Percent","view.count":"Count","view.table":"Table","view.theoretical":"Theory","view.empirical":"Simulation","view.ontime":"On time","view.late":"Late","view.pending":"Not submitted","view.axisPercent":"Share among known outcomes, %","view.axisCount":"Number of responses","view.axisProbability":"Probability, %","view.axisRuns":"Number of runs","view.axisEstimate":"Estimated probability, %","view.threshold":"Event threshold",
      "data.title":"What do the responses show?","data.eyebrow":"DESCRIBING THE SAMPLE","data.subtitle":"Frequencies show how answers are distributed. Comparisons show how outcomes differ across groups.","data.denominator":"Distributions include all 29 eligible responses. The on-time share uses only known outcomes; assignments not submitted yet are shown separately.","data.distributionEyebrow":"ONE VARIABLE","data.distributionTitle":"How often does each answer appear?","data.chooseVariable":"Variable","data.comparisonEyebrow":"OUTCOME COMPARISON","data.comparisonTitle":"How did outcomes split within each group?","data.choosePredictor":"Grouping variable","data.knownOnly":"On-time and late percentages use known outcomes within each group. Pending outcomes remain a separate count.","data.exact":"Exact values","data.snapshotTitle":"Data snapshot","data.snapshotText":"52 responses were collected through Google Forms from 30 September to 2 October 2026. The site publishes aggregate counts, not individual response rows.","data.total":"Total","data.share":"Share of all eligible responses","data.knownN":"Known n","data.onTimePercent":"On time among known outcomes","data.relationshipAccessible":"The table gives exact values and the denominator for every category.","data.variable.allotted":"Time allowed for the assignment","data.variable.start":"When work started","data.variable.outcome":"Submission outcome vs deadline","data.variable.extension":"Was the deadline extended?","data.variable.planning":"Work planning","data.variable.difficulty":"Perceived difficulty","data.variable.otherDeadlines":"Other assignments that week",
      "cat.sameDay":"On the due date","cat.oneDay":"1 day before","cat.twoDays":"2 days before","cat.threeFourDays":"3–4 days before","cat.fiveSevenDays":"5–7 days before","cat.eightPlusDays":"8 or more days before","cat.dontRemember":"Do not remember","cat.dueDate":"On due date, before the deadline time","cat.afterDeadline":"After the original deadline","cat.notStarted":"Had not started","cat.onTime":"By the original deadline","cat.late":"After the original deadline","cat.pending":"Not submitted yet","cat.no":"No","cat.yes":"Yes","cat.unknown":"Do not know or remember","cat.mentalPlan":"Rough plan in mind","cat.noPlan":"No plan","cat.writtenPlan":"Written plan","cat.veryEasy":"Very easy","cat.ratherEasy":"Rather easy","cat.moderate":"Moderately difficult","cat.ratherDifficult":"Rather difficult","cat.veryDifficult":"Very difficult","cat.none":"None","cat.one":"One","cat.two":"Two","cat.three":"Three","cat.fourPlus":"Four or more",
      "lab.title":"Probability Lab","lab.eyebrow":"A BINOMIAL MODEL EXPERIMENT","lab.subtitle":"If the probability of an on-time submission is p, how many students in a group of n might submit on time?","lab.individual":"One student is one trial","lab.group":"X is the number submitted on time in a group","lab.explanation":"Success means submitting by the original deadline. The model gives every student the same probability p and treats outcomes as independent.","lab.setupEyebrow":"EXPERIMENT SETTINGS","lab.setupTitle":"Choose a probability and group size","lab.probabilitySource":"Source for probability p","lab.observed":"Observed in the survey","lab.manual":"Set manually","lab.groupSize":"Group size n","lab.repetitions":"Experiment repetitions","lab.manualProbability":"Success probability p","lab.threshold":"Event of interest","lab.atLeast":"At least","lab.onTime":"submit on time","lab.run":"Run experiment","lab.observedOrigin":"Observed share: 17 of 23 known outcomes; 6 pending responses are excluded.","lab.manualOrigin":"The manual value of p is not calculated from survey responses.","lab.advanced":"Advanced settings","lab.seed":"Random generator seed (optional)","lab.seedHelp":"The same settings and seed reproduce the same result.","lab.resultEyebrow":"EXPERIMENT OUTCOME","lab.resultTitle":"What could happen in a group?","lab.probability":"Success probability","lab.resultGroup":"Size of one group","lab.expected":"Expected on-time submissions","lab.viewDistribution":"Distribution","lab.viewTail":"Tail probability","lab.viewConvergence":"Convergence","lab.viewGroup":"One group","lab.oneGroupTitle":"One random group","lab.oneGroupDescription":"Each marker represents one simulated student.","lab.anotherGroup":"Simulate another group","lab.exact":"Exact values and comparison with theory","lab.assumptionsTitle":"What does the model assume?","lab.assumption1":"Each trial uses the same probability p.","lab.assumption2":"Student outcomes are treated as independent.","lab.assumption3":"Real behaviour depends on many factors that this simple model does not include.","lab.assumption4":"Observed p is estimated from the sample. The simulation illustrates probability, not a causal prediction.","lab.theoryStory":"What theory expects","lab.simStory":"What the simulation produced","lab.compareStory":"Theory and simulation","lab.interpretation":"Interpretation","lab.expectedValue":"Expected value","lab.average":"Simulation average","lab.theoreticalTail":"Theoretical P(X ≥ k)","lab.simulatedTail":"Simulated P(X ≥ k)","lab.difference":"Absolute difference","lab.interpretClose":"The simulation estimate is close to the theoretical value. More repetitions usually reduce random error, but the estimate does not have to improve at every step.","lab.interpretFar":"The gap between the simulation estimate and theory is part of random variation. More repetitions often make the estimate steadier, but convergence is not monotonic.","lab.runs":"groups simulated","lab.studentSuccess":"Student {n}: success, submitted on time","lab.studentFailure":"Student {n}: failure in the model","lab.groupOutcome":"{success} of {n} students submitted on time in this random group.","lab.chartDistribution":"Distribution of on-time submissions per group","lab.chartTail":"Outcome probabilities; amber marks values X ≥ {k}","lab.chartConvergence":"Estimate of P(X ≥ {k}) as repetitions increase","lab.chartGroup":"One group of {n} students was simulated.","lab.tableX":"Number submitted on time, X","lab.tableTheory":"Theoretical probability","lab.tableSimulation":"Simulation frequency","lab.tableThreshold":"In tail",
      "method.title":"Formulas and Methods","method.eyebrow":"COURSE FORMULA REFERENCE","method.subtitle":"A concise guide to frequency distributions, events, conditional probability, independence, Bayes' theorem, and random variables.","method.separate":"This is a standalone study reference. It does not insert survey values into formulas or interpret survey answers.","method.navFrequency":"Frequencies","method.navEvents":"Events","method.navConditional":"Conditional probability","method.navBayes":"Bayes","method.navVariables":"Random variables","method.navDistributions":"Distributions","method.frequencyTitle":"Frequency distributions","method.frequencyIntro":"Group observations by value or interval, then describe each group with a count and a share.","method.absolute":"Absolute frequency","method.absoluteText":"The number of times a category appears.","method.relative":"Relative frequency","method.relativeText":"The category count divided by the total number n of observations.","method.percent":"Percentage frequency","method.percentText":"Relative frequency expressed as a percentage.","method.cumulative":"Cumulative frequency","method.cumulativeText":"The sum of frequencies up to a selected value; useful for ordered data.","method.range":"Range","method.rangeText":"The difference between the largest and smallest numeric observations.","method.sturges":"Sturges' rule","method.sturgesText":"An approximate number of intervals for grouping quantitative data.","method.frequencyFootnote":"Count categorical answers directly. A broad interval does not give an exact numeric value.","method.eventsTitle":"Sets and events","method.eventsIntro":"A random event is a subset of the sample space Ω.","method.union":"Union","method.unionText":"A or B occurs, including the case where both occur.","method.intersection":"Intersection","method.intersectionText":"Both A and B occur at the same time.","method.complement":"Complement","method.complementText":"The probability that A does not occur.","method.addition":"General addition rule","method.additionText":"Subtract the intersection so shared outcomes are not counted twice.","method.disjoint":"Mutually exclusive events","method.disjointText":"If two events cannot occur together, add their probabilities.","method.equiprobable":"Equally likely outcomes","method.equiprobableText":"When outcomes are equally likely, divide favourable outcomes by all outcomes.","method.conditionalTitle":"Conditional probability and independence","method.conditionalIntro":"Conditional probability narrows the sample space after event B is known.","method.conditional":"Conditional probability","method.conditionalText":"The probability of A given that B has occurred.","method.multiplication":"Multiplication rule","method.multiplicationText":"Connects joint probability with conditional probability.","method.independent":"Independent events","method.independentText":"The occurrence of one event does not change the probability of the other.","method.independentCheck":"Independence check","method.independentCheckText":"When P(B) > 0, this is equivalent to A and B being independent.","method.distinctionTitle":"Keep these distinct","method.distinction":"Mutually exclusive means A and B cannot happen together. Independent means one does not change the probability of the other. These are different properties.","method.bayesTitle":"Total probability and Bayes' theorem","method.bayesIntro":"If several mutually exclusive causes A₁,…,Aₖ are possible, the probability of outcome B is the sum of their contributions.","method.totalProbability":"Law of total probability","method.totalProbabilityText":"A₁,…,Aₖ form a complete set of events.","method.bayes":"Bayes' theorem","method.bayesText":"Updates the probability of cause Aⱼ after outcome B is observed.","method.variablesTitle":"Discrete random variables","method.variablesIntro":"A random variable X assigns a number to each outcome and has a probability distribution.","method.pmf":"Probability mass function","method.pmfText":"Probabilities are non-negative and sum to one over all possible values.","method.expectation":"Expected value","method.expectationText":"The long-run probability-weighted average.","method.variance":"Variance and standard deviation","method.varianceText":"Describe the spread of values around the expected value.","method.distributionsTitle":"Common distributions","method.distributionsIntro":"Choose a model based on how the random variable is generated and on the assumptions of the problem.","method.binomial":"Binomial distribution","method.binomialText":"The number of successes in n independent trials with the same probability p.","method.poisson":"Poisson distribution","method.poissonText":"Models the number of events in a fixed interval with rate parameter λ.","method.normal":"Normal distribution","method.normalText":"A continuous, symmetric distribution centred at μ with scale σ.","method.zscore":"Standardisation","method.zscoreText":"Shows how many standard deviations a value is from the mean; for a normal model, Z has distribution N(0,1).","method.sources":"Based on course materials about frequency distributions, sets and events, conditional probability, independent events, Bayes' theorem, and random variables.","method.dataLink":"Open the study data"
    }
  };

  Object.assign(copy.ru, {
    "overview.context":"Контекст выборки", "overview.sourceAndLimits":"Источник данных и ограничения", "view.chartType":"Вид диаграммы", "data.context":"Контекст данных", "method.index":"Разделы справочника", "lab.decrease":"Уменьшить", "lab.increase":"Увеличить"
  });
  Object.assign(copy.kk, {
    "overview.context":"Іріктеме туралы мәлімет", "overview.sourceAndLimits":"Дереккөз және шектеулер", "view.chartType":"Диаграмма көрінісі", "data.context":"Деректер туралы мәлімет", "method.index":"Анықтамалық бөлімдері", "lab.decrease":"Азайту", "lab.increase":"Көбейту"
  });
  Object.assign(copy.en, {
    "brand.title":"Deadline Dynamics", "overview.context":"Sample context", "overview.sourceAndLimits":"Data source and limitations", "view.chartType":"Chart view", "data.context":"Data context", "method.index":"Reference sections", "lab.decrease":"Decrease", "lab.increase":"Increase"
  });

  const externalCopy = window.DEADLINE_COPY_OVERRIDES || {};
  ["ru", "kk", "en"].forEach(locale => Object.assign(copy[locale], externalCopy[locale] || {}));

  const langParam = new URLSearchParams(window.location.search).get("lang");
  let language = ["ru", "kk", "en"].includes(langParam) ? langParam : localStorage.getItem("deadline-language") || "ru";
  localStorage.setItem("deadline-language", language);
  const t = key => (copy[language] && copy[language][key]) || copy.ru[key] || key;
  const formatNumber = (value, digits = 0) => Number.isFinite(value) ? new Intl.NumberFormat(language, { minimumFractionDigits: digits, maximumFractionDigits: digits }).format(value) : t("common.noData");
  const formatPercent = value => Number.isFinite(value) ? `${formatNumber(value, 1)}%` : t("common.noData");
  const interpolate = (template, values) => template.replace(/\{(\w+)\}/g, (_, key) => values[key] ?? "");
  const getCategory = code => t(`cat.${code}`);

  function applyLanguage() {
    document.documentElement.lang = language;
    document.querySelectorAll("[data-i18n]").forEach(element => { element.textContent = t(element.dataset.i18n); });
    document.querySelectorAll("[data-i18n-aria]").forEach(element => { element.setAttribute("aria-label", t(element.dataset.i18nAria)); });
    document.querySelectorAll("[data-language]").forEach(link => {
      const selected = link.dataset.language === language;
      if (selected) link.setAttribute("aria-current", "true"); else link.removeAttribute("aria-current");
      link.href = `${window.location.pathname}?lang=${link.dataset.language}`;
    });
    document.querySelectorAll("a[data-local-link]").forEach(link => {
      const url = new URL(link.getAttribute("href"), window.location.origin);
      url.searchParams.set("lang", language);
      link.href = `${url.pathname}${url.search}${url.hash}`;
    });
    document.querySelectorAll(".main-nav").forEach(nav => nav.setAttribute("aria-label", t("common.navigation")));
    document.querySelectorAll(".language-switcher").forEach(nav => nav.setAttribute("aria-label", t("common.language")));
    document.querySelectorAll("[data-nav-toggle]").forEach(button => button.setAttribute("aria-label", t("common.open")));
    document.querySelectorAll("[data-nav-close]").forEach(button => button.setAttribute("aria-label", t("common.close")));
    const page = document.querySelector("[data-page]")?.dataset.page || "overview";
    const titles = { overview: "overview.title", data: "data.title", simulator: "lab.title", method: "method.title" };
    document.title = `${t(titles[page])} | ${t("brand.title")}`;
    const overallRate = document.getElementById("overallRate");
    if (overallRate) overallRate.textContent = formatPercent(100 * study.onTime / study.known);
    updateFixedLabels();
  }

  function updateFixedLabels() {
    const languageText = {
      "#overviewChart": "overview.patternTitle", "#distributionChart": "data.distributionTitle", "#relationshipChart": "data.comparisonTitle", "#simulatorChart": "lab.chartDistribution"
    };
    Object.entries(languageText).forEach(([selector, key]) => document.querySelector(selector)?.setAttribute("aria-label", t(key)));
    document.querySelectorAll("[data-step]").forEach(button => button.setAttribute("aria-label", `${t(button.dataset.step === "1" ? "lab.increase" : "lab.decrease")} ${t("lab.groupSize")}`));
  }

  applyLanguage();

  const palette = { teal: "#49bfae", blue: "#5da6bd", amber: "#d0a03a", coral: "#c8766f", sage: "#7f9d86", muted: "#929a96", grid: "rgba(255,255,255,.075)" };
  if (window.Chart) {
    Chart.defaults.color = palette.muted;
    Chart.defaults.font.family = 'Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif';
    Chart.defaults.font.size = 11;
    Chart.defaults.animation = false;
  }

  function makeTable(container, headers, rows, caption) {
    container.replaceChildren();
    const table = document.createElement("table");
    const cap = document.createElement("caption");
    cap.className = "sr-only";
    cap.textContent = caption;
    table.append(cap);
    const thead = document.createElement("thead");
    const headerRow = document.createElement("tr");
    headers.forEach((label, index) => {
      const th = document.createElement("th");
      th.scope = "col";
      th.textContent = label;
      if (index > 0) th.style.textAlign = "right";
      headerRow.append(th);
    });
    thead.append(headerRow);
    const tbody = document.createElement("tbody");
    rows.forEach(row => {
      const tr = document.createElement("tr");
      row.forEach((value, index) => {
        const cell = document.createElement(index === 0 ? "th" : "td");
        if (index === 0) cell.scope = "row";
        cell.textContent = String(value);
        tr.append(cell);
      });
      tbody.append(tr);
    });
    table.append(thead, tbody);
    container.append(table);
  }

  function chartTooltip() {
    return {
      backgroundColor: "#121615", borderColor: "rgba(255,255,255,.14)", borderWidth: 1,
      titleColor: "#e8ece9", bodyColor: "#c0c7c3", padding: 10, cornerRadius: 6,
      titleFont: { size: 11, weight: "600" }, bodyFont: { size: 11 }, position: "nearest",
      callbacks: { title: items => {
        if (!items.length) return "";
        const { chart, label } = items[0];
        const maxWidth = Math.max(80, Math.min(300, chart.width - 44));
        const lines = [""];
        chart.ctx.save();
        chart.ctx.font = `600 11px ${Chart.defaults.font.family}`;
        String(label).split(/\s+/).forEach(word => {
          const last = lines.length - 1;
          const next = lines[last] ? `${lines[last]} ${word}` : word;
          if (lines[last] && chart.ctx.measureText(next).width > maxWidth) lines.push(word);
          else lines[last] = next;
        });
        chart.ctx.restore();
        return lines;
      } }
    };
  }

  function commonChartOptions(yLabel, max) {
    return {
      responsive: true, maintainAspectRatio: false, animation: false,
      interaction: { mode: "index", axis: "x", intersect: false },
      plugins: {
        legend: { position: "bottom", labels: { boxWidth: 9, boxHeight: 9, usePointStyle: true, pointStyle: "circle", padding: 15 } },
        tooltip: chartTooltip()
      },
      scales: {
        x: { grid: { color: palette.grid }, ticks: { color: palette.muted, maxRotation: 0, autoSkip: false } },
        y: { beginAtZero: true, max, grid: { color: palette.grid }, ticks: { color: palette.muted }, title: { display: true, text: yLabel, color: palette.muted, font: { size: 10 } } }
      }
    };
  }

  function horizontalOptions(xLabel, max) {
    return {
      responsive: true, maintainAspectRatio: false, indexAxis: "y", animation: false,
      interaction: { mode: "index", axis: "y", intersect: false },
      plugins: { legend: { position: "bottom", labels: { boxWidth: 9, boxHeight: 9, usePointStyle: true, pointStyle: "circle", padding: 15 } }, tooltip: chartTooltip() },
      scales: {
        x: { beginAtZero: true, max, stacked: true, grid: { color: palette.grid }, ticks: { color: palette.muted, callback: value => max === 100 ? `${value}%` : value }, title: { display: true, text: xLabel, color: palette.muted, font: { size: 10 } } },
        y: { stacked: true, grid: { display: false }, ticks: { color: palette.muted, autoSkip: false } }
      }
    };
  }

  const destroyChart = chart => { if (chart) chart.destroy(); return null; };

  function updateOverview() {
    const canvas = document.getElementById("overviewChart");
    if (!canvas) return;
    const sections = document.querySelectorAll("[data-overview-view]");
    const exact = document.getElementById("overviewExact");
    const start = study.variables.find(item => item.key === "start");
    const view = document.querySelector('[data-overview-view][aria-pressed="true"]')?.dataset.overviewView || "percentage";
    const known = start.categories.map(item => item.onTime + item.late);
    const labels = start.categories.map((item, i) => `${getCategory(item.code)} (${t("common.n")}=${known[i]})`);
    const onTime = start.categories.map((item, i) => view === "percentage" ? (known[i] ? 100 * item.onTime / known[i] : 0) : item.onTime);
    const late = start.categories.map((item, i) => view === "percentage" ? (known[i] ? 100 * item.late / known[i] : 0) : item.late);
    const exactRows = start.categories.map((item, i) => [getCategory(item.code), item.count, known[i], item.onTime, item.late, item.pending, known[i] ? formatPercent(100 * item.onTime / known[i]) : t("common.noData")]);
    makeTable(exact, [t("common.category"), t("data.total"), t("data.knownN"), t("common.onTime"), t("common.late"), t("common.pending"), t("data.onTimePercent")], exactRows, t("overview.patternTitle"));
    const accessible = start.categories.map((item, i) => `${labels[i]}: ${item.onTime}/${known[i]} ${t("common.onTime").toLowerCase()}`).join("; ");
    document.getElementById("overviewChartText").textContent = `${t("overview.knownDenominator")}. ${accessible}.`;
    document.getElementById("overviewChart").closest(".chart-frame").hidden = view === "table";
    document.getElementById("overviewExact").parentElement.open = view === "table";
    if (view === "table") { window.overviewChartInstance = destroyChart(window.overviewChartInstance); return; }
    window.overviewChartInstance = destroyChart(window.overviewChartInstance);
    window.overviewChartInstance = new Chart(canvas, {
      type: "bar",
      data: { labels, datasets: [
        { label: t("view.ontime"), data: onTime, backgroundColor: "rgba(73,191,174,.82)", borderColor: palette.teal, borderWidth: 1, borderRadius: 2 },
        { label: t("view.late"), data: late, backgroundColor: "rgba(200,118,111,.72)", borderColor: palette.coral, borderWidth: 1, borderRadius: 2 }
      ] },
      options: { ...horizontalOptions(view === "percentage" ? t("view.axisPercent") : t("view.axisCount"), view === "percentage" ? 100 : undefined), scales: { ...horizontalOptions("", view === "percentage" ? 100 : undefined).scales, y: { ...horizontalOptions("", undefined).scales.y, stacked: true }, x: { ...horizontalOptions("", view === "percentage" ? 100 : undefined).scales.x, stacked: true } } }
    });
    sections.forEach(button => button.setAttribute("aria-pressed", String(button.dataset.overviewView === view)));
  }

  function initOverview() {
    if (!document.getElementById("overviewChart")) return;
    document.querySelectorAll("[data-count]").forEach(element => {
      const value = study[element.dataset.count];
      if (Number.isFinite(value)) element.textContent = formatNumber(value);
    });
    document.getElementById("overallRate").textContent = formatPercent(100 * study.onTime / study.known);
    document.getElementById("overallFraction").textContent = interpolate(t("overview.knownFraction"), {
      onTime: formatNumber(study.onTime), known: formatNumber(study.known)
    });
    const sourcePeriod = document.getElementById("sourcePeriod");
    if (sourcePeriod && study.collectedFrom && study.collectedTo) {
      sourcePeriod.hidden = false;
      sourcePeriod.textContent = interpolate(t("overview.sourcePeriod"), { from: study.collectedFrom, to: study.collectedTo });
    }
    document.querySelectorAll("[data-overview-view]").forEach(button => button.addEventListener("click", () => {
      document.querySelectorAll("[data-overview-view]").forEach(item => item.setAttribute("aria-pressed", String(item === button)));
      updateOverview();
    }));
    updateOverview();
  }

  function initData() {
    const host = document.getElementById("dataChapters");
    if (!host) return;
    const order = ["allotted", "start", "outcome", "extension", "planning", "difficulty", "otherDeadlines"];
    const distributionViews = {
      allotted: ["simpleBar", "line", "table"], start: ["line", "simpleBar", "table"],
      outcome: ["pie", "simpleBar", "table"], extension: ["percentageBar", "pie", "table"],
      planning: ["horizontalBar", "pie", "table"], difficulty: ["line", "simpleBar", "table"],
      otherDeadlines: ["simpleBar", "line", "table"]
    };
    const comparisons = new Set(["allotted", "start", "planning", "difficulty", "otherDeadlines"]);
    const variables = order.map(key => study.variables.find(item => item.key === key)).filter(Boolean);
    const colorFor = code => ({
      onTime: palette.teal, late: palette.coral, pending: palette.muted,
      yes: palette.blue, no: palette.teal, unknown: palette.muted,
      sameDay: palette.teal, oneDay: palette.blue, twoDays: palette.amber,
      threeFourDays: palette.purple, fiveSevenDays: palette.sage, eightPlusDays: palette.coral,
      mentalPlan: palette.blue, noPlan: palette.teal, writtenPlan: palette.sage, notStarted: palette.muted,
      dontRemember: palette.muted,
      veryEasy: palette.sage, ratherEasy: palette.teal, moderate: palette.blue,
      ratherDifficult: palette.amber, veryDifficult: palette.coral
    })[code] || palette.teal;

    const valueLabels = {
      id: "deadlineDataValueLabels",
      afterDatasetsDraw(chart, args, settings = {}) {
        const { ctx } = chart;
        ctx.save();
        ctx.font = "600 10px Inter, system-ui, sans-serif";
        ctx.textBaseline = "middle";
        chart.data.datasets.forEach((dataset, datasetIndex) => {
          const meta = chart.getDatasetMeta(datasetIndex);
          if (meta.hidden) return;
          meta.data.forEach((item, index) => {
            const value = Number(dataset.data[index]);
            if (!Number.isFinite(value) || value <= 0) return;
            if (chart.config.type === "pie") {
              const total = dataset.data.reduce((sum, current) => sum + Number(current || 0), 0);
              const percent = total ? value / total * 100 : 0;
              if (percent < 8) return;
              const point = item.getCenterPoint();
              ctx.fillStyle = "#080a09";
              ctx.textAlign = "center";
              ctx.fillText(`${formatNumber(percent, 0)}%`, point.x, point.y);
              return;
            }
            if (settings.mode === "percentage" && value < 10) return;
            const point = item.getProps(["x", "y", "base"], true);
            ctx.fillStyle = "#e8ece9";
            if (chart.options.indexAxis === "y") {
              const width = Math.abs(point.x - point.base);
              if (width < 24) return;
              ctx.textAlign = "right";
              ctx.fillText(settings.mode === "percentage" ? `${formatNumber(value, 0)}%` : formatNumber(value), point.x - 5, point.y);
            } else if (chart.config.type === "line") {
              ctx.textAlign = "center";
              ctx.fillText(formatNumber(value), point.x, point.y - 11);
            } else {
              ctx.textAlign = "center";
              ctx.fillText(formatNumber(value), point.x, point.y - 9);
            }
          });
        });
        ctx.restore();
      }
    };

    function chartHeight(state) {
      if (state.kind === "comparison") return Math.min(360, Math.max(190, state.variable.categories.length * 46 + 72));
      if (state.activeView === "pie") return 270;
      if (state.activeView === "percentageBar") return 166;
      if (state.activeView === "horizontalBar") return Math.min(350, Math.max(250, state.variable.categories.length * 48 + 75));
      if (state.activeView === "line") return state.variable.key === "start" ? 330 : 276;
      if (state.variable.key === "otherDeadlines") return 252;
      return state.variable.key === "allotted" ? 280 : 296;
    }

    function percentOfEligible(count) {
      return study.eligible ? formatPercent(100 * count / study.eligible) : t("common.noData");
    }

    function wrapLabel(label, limit) {
      const words = String(label).split(/\s+/);
      const lines = [];
      let line = "";
      words.forEach(word => {
        if (line && `${line} ${word}`.length > limit) {
          lines.push(line);
          line = word;
        } else line = line ? `${line} ${word}` : word;
      });
      if (line) lines.push(line);
      return lines;
    }

    function element(tag, className, text) {
      const node = document.createElement(tag);
      if (className) node.className = className;
      if (text !== undefined) node.textContent = text;
      return node;
    }

    function addViewButtons(container, views, recommended, ariaLabel) {
      const group = element("div", "view-switcher local-view-switcher");
      group.setAttribute("role", "group");
      group.setAttribute("aria-label", ariaLabel);
      views.forEach(view => {
        const button = element("button", "", t(`view.${view}`));
        button.type = "button";
        button.dataset.view = view;
        button.setAttribute("aria-pressed", String(view === recommended));
        if (view === recommended) button.append(element("small", "recommended", t("data.recommended")));
        group.append(button);
      });
      container.append(group);
      return group;
    }

    function addWorkspace(parent, options) {
      const workspace = element("section", "data-workspace-section");
      const heading = element("h3", "data-workspace-title", options.title);
      workspace.append(heading);
      const controls = addViewButtons(workspace, options.views, options.defaultView, options.ariaLabel);
      const frame = element("div", "chart-frame data-chart-frame");
      const canvas = element("canvas", "");
      canvas.id = options.id;
      canvas.setAttribute("role", "img");
      canvas.setAttribute("aria-label", options.title);
      frame.append(canvas);
      workspace.append(frame);
      const text = element("p", "sr-only");
      text.setAttribute("aria-live", "polite");
      workspace.append(text);
      const table = element("div", "table-scroll");
      table.tabIndex = 0;
      table.hidden = options.defaultView !== "table";
      workspace.append(table);
      parent.append(workspace);
      return { workspace, controls, frame, canvas, text, table, chart: null, activeView: options.defaultView, variable: options.variable, kind: options.kind };
    }

    function updatePressed(state) {
      state.controls.querySelectorAll("button").forEach(button => button.setAttribute("aria-pressed", String(button.dataset.view === state.activeView)));
      state.frame.dataset.visual = state.activeView;
      state.frame.style.height = `${chartHeight(state)}px`;
      state.frame.hidden = state.activeView === "table";
      state.table.hidden = state.activeView !== "table";
      state.chart = destroyChart(state.chart);
    }

    function renderDistribution(state) {
      const variable = state.variable;
      const labels = variable.categories.map(item => getCategory(item.code));
      const counts = variable.categories.map(item => item.count);
      const rows = variable.categories.map((item, i) => [labels[i], item.count, percentOfEligible(item.count)]);
      makeTable(state.table, [t("common.category"), t("data.total"), t("data.share")], rows, t(`data.variable.${variable.key}`));
      state.text.textContent = variable.categories.map((item, i) => `${labels[i]}: ${formatNumber(item.count)} (${percentOfEligible(item.count)})`).join("; ");
      state.canvas.setAttribute("aria-label", t(`data.variable.${variable.key}`));
      updatePressed(state);
      if (state.activeView === "table" || !window.Chart) return;
      const paletteColors = variable.categories.map(item => colorFor(item.code));
      if (state.activeView === "percentageBar") {
        const chartLabels = labels.map(label => label);
        state.chart = new Chart(state.canvas, {
          type: "bar",
          data: { labels: [t("data.responses")], datasets: variable.categories.map((item, index) => ({
            label: labels[index], data: [study.eligible ? 100 * item.count / study.eligible : 0],
            count: item.count, backgroundColor: paletteColors[index], borderColor: "#0f1211", borderWidth: 1
          })) },
          plugins: [valueLabels],
          options: { responsive: true, maintainAspectRatio: false, animation: false, indexAxis: "y", interaction: { mode: "nearest", intersect: true },
            plugins: { valueLabels: { mode: "percentage" }, legend: { position: "bottom", labels: { color: palette.muted, boxWidth: 9, usePointStyle: true, pointStyle: "circle", padding: 10 } },
              tooltip: { ...chartTooltip(), callbacks: { title: () => "", label: context => `${chartLabels[context.datasetIndex]}: ${formatNumber(variable.categories[context.datasetIndex].count)} (${percentOfEligible(variable.categories[context.datasetIndex].count)})` } } },
            scales: { x: { stacked: true, min: 0, max: 100, grid: { color: palette.grid }, ticks: { color: palette.muted, maxTicksLimit: 5, callback: value => `${value}%` }, title: { display: true, text: t("data.axisShare"), color: palette.muted, font: { size: 10 } } },
              y: { stacked: true, grid: { display: false }, ticks: { display: false } } }
          }
        });
        return;
      }
      if (state.activeView === "pie") {
        state.chart = new Chart(state.canvas, {
          type: "pie", data: { labels, datasets: [{ data: counts, backgroundColor: paletteColors, borderColor: "#0f1211", borderWidth: 2 }] },
          plugins: [valueLabels],
          options: { responsive: true, maintainAspectRatio: false, animation: false, plugins: {
            valueLabels: { mode: "pie" },
            legend: { position: "bottom", labels: { color: palette.muted, boxWidth: 9, usePointStyle: true, pointStyle: "circle", padding: 10,
              generateLabels: () => labels.map((label, index) => ({ text: `${label} · ${formatNumber(counts[index])} (${percentOfEligible(counts[index])})`, fillStyle: paletteColors[index], strokeStyle: "#0f1211", lineWidth: 2, fontColor: palette.muted, hidden: false, index, datasetIndex: 0 })) } },
            tooltip: { ...chartTooltip(), callbacks: { ...chartTooltip().callbacks, label: context => `${formatNumber(context.raw)} (${percentOfEligible(context.raw)})` } }
          } }
        });
        return;
      }
      if (state.activeView === "line") {
        const options = commonChartOptions(t("data.axisCount"), undefined);
        options.scales.x.grid.display = false;
        options.scales.x.ticks.maxRotation = state.variable.key === "difficulty" ? 0 : 35;
        options.scales.x.ticks.minRotation = 0;
        options.scales.x.ticks.autoSkip = false;
        options.scales.x.ticks.callback = (value, index) => state.variable.key === "difficulty" ? String(index + 1) : labels[index];
        options.scales.y.ticks.precision = 0;
        options.scales.y.ticks.maxTicksLimit = 5;
        options.plugins.legend.display = false;
        options.plugins.tooltip.callbacks.label = context => `${t("data.responses")}: ${formatNumber(context.raw)} (${percentOfEligible(context.raw)})`;
        state.chart = new Chart(state.canvas, { type: "line", data: { labels, datasets: [{ label: t("data.responses"), data: counts, borderColor: palette.teal, backgroundColor: "rgba(73,191,174,.08)", borderWidth: 2, pointRadius: 4, pointHoverRadius: 5, tension: 0, stepped: state.variable.key === "start" ? "middle" : false }] }, options, plugins: [valueLabels] });
        return;
      }
      const horizontal = state.activeView === "horizontalBar" || state.variable.key === "planning";
      const chartLabels = horizontal ? variable.categories.map(item => wrapLabel(getCategory(item.code), 17)) : labels;
      if (horizontal) {
        const options = horizontalOptions(t("data.axisCount"), undefined);
        options.scales.x.stacked = false;
        options.scales.y.stacked = false;
        options.scales.x.grid.color = palette.grid;
        options.scales.x.ticks.precision = 0;
        options.scales.x.ticks.maxTicksLimit = 5;
        options.scales.y.ticks.autoSkip = false;
        options.plugins.legend.display = false;
        options.plugins.tooltip.callbacks.label = context => `${formatNumber(context.raw)} (${percentOfEligible(context.raw)})`;
        state.chart = new Chart(state.canvas, {
          type: "bar", data: { labels: chartLabels, datasets: [{ label: t("data.responses"), data: counts, backgroundColor: "rgba(73,191,174,.78)", borderColor: palette.teal, borderWidth: 1, borderRadius: 2, categoryPercentage: .72, barPercentage: .78 }] }, options, plugins: [valueLabels]
        });
        return;
      }
      const options = commonChartOptions(t("data.axisCount"), undefined);
      options.scales.x.grid.display = false;
      options.scales.x.ticks.maxRotation = 40;
      options.scales.x.ticks.minRotation = 0;
      options.scales.x.ticks.autoSkip = false;
      options.scales.y.grid.color = palette.grid;
      options.scales.y.ticks.precision = 0;
      options.scales.y.ticks.maxTicksLimit = 5;
      options.plugins.legend.display = false;
      options.plugins.tooltip.callbacks.label = context => `${formatNumber(context.raw)} (${percentOfEligible(context.raw)})`;
      state.chart = new Chart(state.canvas, {
        type: "bar", data: { labels: chartLabels, datasets: [{ label: t("data.responses"), data: counts, backgroundColor: "rgba(73,191,174,.78)", borderColor: palette.teal, borderWidth: 1, borderRadius: 2, categoryPercentage: state.variable.key === "otherDeadlines" ? .68 : .76, barPercentage: state.variable.key === "otherDeadlines" ? .68 : .82 }] },
        options, plugins: [valueLabels]
      });
    }

    function renderComparison(state) {
      const variable = state.variable;
      const known = variable.categories.map(item => item.onTime + item.late);
      const categoryLabels = variable.categories.map((item, i) => `${getCategory(item.code)} (${t("common.n")}=${known[i]})`);
      const rows = variable.categories.map((item, i) => [getCategory(item.code), item.onTime, item.late, known[i], known[i] ? formatPercent(100 * item.onTime / known[i]) : t("common.noData"), item.pending]);
      makeTable(state.table, [t("common.category"), t("common.onTime"), t("common.late"), t("data.knownN"), t("data.onTimeShare"), t("common.pending")], rows, `${t("data.outcomeComparison")}: ${t(`data.variable.${variable.key}`)}`);
      state.text.textContent = variable.categories.map((item, i) => known[i]
        ? `${getCategory(item.code)}: n=${known[i]}, ${t("common.onTime")} ${item.onTime} (${formatPercent(100 * item.onTime / known[i])}), ${t("common.late")} ${item.late} (${formatPercent(100 * item.late / known[i])})`
        : `${getCategory(item.code)}: ${t("data.knownN")} = 0`).join("; ");
      state.canvas.setAttribute("aria-label", `${t("data.outcomeComparison")}: ${t(`data.variable.${variable.key}`)}`);
      updatePressed(state);
      if (state.activeView === "table" || !window.Chart) return;
      const percentage = state.activeView === "percentageBar";
      const max = percentage ? 100 : Math.max(1, ...known);
      const options = horizontalOptions(percentage ? t("data.axisShare") : t("data.axisCount"), max);
      options.scales.x.stacked = percentage;
      options.scales.y.stacked = percentage;
      options.scales.x.ticks.callback = value => percentage ? `${value}%` : formatNumber(value);
      options.scales.y.ticks.autoSkip = false;
      options.scales.x.ticks.maxTicksLimit = percentage ? 5 : 6;
      options.scales.y.grid.display = false;
      options.plugins.legend.labels.padding = 10;
      options.plugins.valueLabels = { mode: percentage ? "percentage" : "count" };
      state.chart = new Chart(state.canvas, {
        type: "bar", data: { labels: categoryLabels, datasets: [
          { label: t("common.onTime"), data: variable.categories.map((item, i) => known[i] && percentage ? 100 * item.onTime / known[i] : item.onTime), counts: variable.categories.map(item => item.onTime), backgroundColor: "rgba(73,191,174,.84)", borderColor: palette.teal, borderWidth: 1, borderRadius: 2 },
          { label: t("common.late"), data: variable.categories.map((item, i) => known[i] && percentage ? 100 * item.late / known[i] : item.late), counts: variable.categories.map(item => item.late), backgroundColor: "rgba(200,118,111,.8)", borderColor: palette.coral, borderWidth: 1, borderRadius: 2 }
        ] }, options: { ...options, plugins: { ...options.plugins, tooltip: { ...options.plugins.tooltip, callbacks: {
          ...options.plugins.tooltip.callbacks,
          label: context => {
            const item = variable.categories[context.dataIndex];
            const count = context.datasetIndex === 0 ? item.onTime : item.late;
            const share = known[context.dataIndex] ? formatPercent(100 * count / known[context.dataIndex]) : t("common.noData");
            return `${context.dataset.label}: ${formatNumber(count)} (${share})`;
          }
        } } } }, plugins: [valueLabels]
      });
    }

    variables.forEach((variable, index) => {
      const chapter = element("section", "data-chapter");
      chapter.id = `data-${variable.key}`;
      chapter.setAttribute("aria-labelledby", `data-title-${variable.key}`);
      const heading = element("header", "data-chapter-heading");
      heading.append(element("span", "data-chapter-number", String(index + 1).padStart(2, "0")));
      const headingText = element("div", "");
      headingText.append(element("p", "eyebrow", t("data.eyebrow")));
      const title = element("h2", "", t(`data.variable.${variable.key}`));
      title.id = `data-title-${variable.key}`;
      headingText.append(title);
      heading.append(headingText);
      chapter.append(heading);
      const distribution = addWorkspace(chapter, {
        id: `distribution-${variable.key}`, title: t("data.responses"), variable, kind: "distribution",
        views: distributionViews[variable.key], defaultView: distributionViews[variable.key][0],
        ariaLabel: t("data.distributionViews")
      });
      distribution.controls.addEventListener("click", event => {
        const button = event.target.closest("button[data-view]");
        if (!button) return;
        distribution.activeView = button.dataset.view;
        renderDistribution(distribution);
      });
      host.append(chapter);
      renderDistribution(distribution);
      if (comparisons.has(variable.key)) {
        const comparisonHeading = element("div", "data-comparison-heading");
        comparisonHeading.append(element("h3", "", t("data.outcomeComparison")));
        chapter.append(comparisonHeading);
        const comparison = addWorkspace(chapter, {
          id: `comparison-${variable.key}`, title: t("data.outcomeComparison"), variable, kind: "comparison",
          views: ["percentageBar", "multipleBar", "table"], defaultView: "percentageBar",
          ariaLabel: t("data.comparisonViews")
        });
        comparison.controls.addEventListener("click", event => {
          const button = event.target.closest("button[data-view]");
          if (!button) return;
          comparison.activeView = button.dataset.view;
          renderComparison(comparison);
        });
        renderComparison(comparison);
      }
    });
  }

  function randomSeed() {
    if (window.crypto && window.crypto.getRandomValues) {
      const values = new Uint32Array(1);
      window.crypto.getRandomValues(values);
      return values[0];
    }
    return Math.floor(Math.random() * 4294967296) >>> 0;
  }

  function initSimulator() {
    const runButton = document.getElementById("runExperiment");
    if (!runButton) return;
    const observedP = study.onTime / study.known;
    const mode = document.getElementById("probabilityMode");
    const slider = document.getElementById("probabilitySlider");
    const sizeInput = document.getElementById("groupSize");
    const thresholdInput = document.getElementById("threshold");
    const resultPanel = document.getElementById("experimentResults");
    const story = document.getElementById("simulationStory");
    const canvas = document.getElementById("simulatorChart");
    const chartFrame = document.getElementById("simChartFrame");
    const groupPanel = document.getElementById("singleGroupPanel");
    let results = null;
    let chart = null;
    let rng = Math.random;
    let activeView = "distribution";

    function currentP() { return mode.value === "observed" ? observedP : Number(slider.value) / 100; }
    function updateControls() {
      const isManual = mode.value === "manual";
      document.getElementById("manualProbabilityWrap").hidden = !isManual;
      const origin = document.getElementById("observedOrigin");
      if (origin) origin.textContent = isManual ? t("lab.manualOrigin") : interpolate(t("lab.observedOrigin"), { onTime: study.onTime, known: study.known });
      document.getElementById("probabilityOutput").textContent = `${slider.value}%`;
      const n = Number(sizeInput.value);
      thresholdInput.max = String(Number.isInteger(n) && n >= 1 && n <= 100 ? n : 100);
      if (Number(thresholdInput.value) > Number(thresholdInput.max)) thresholdInput.value = thresholdInput.max;
    }
    mode.addEventListener("change", updateControls);
    slider.addEventListener("input", updateControls);
    sizeInput.addEventListener("input", updateControls);
    document.querySelectorAll("[data-step]").forEach(button => button.addEventListener("click", () => {
      const n = Number(sizeInput.value) || 20;
      sizeInput.value = String(Math.max(1, Math.min(100, n + Number(button.dataset.step))));
      updateControls();
    }));
    updateControls();

    function runExperiment() {
      const n = Math.max(1, Math.min(100, Math.round(Number(sizeInput.value) || 20)));
      const repetitions = [1000, 10000, 50000].includes(Number(document.getElementById("repetitions").value)) ? Number(document.getElementById("repetitions").value) : 10000;
      const k = Math.max(0, Math.min(n, Math.round(Number(thresholdInput.value) || 0)));
      const p = Math.max(0, Math.min(1, currentP()));
      sizeInput.value = String(n);
      thresholdInput.value = String(k);
      thresholdInput.max = String(n);
      const simulation = window.DeadlineProbability.simulateBinomial({ n, p, runs: repetitions, threshold: k, seed: randomSeed() });
      rng = simulation.random;
      results = { n, p, k, repetitions, ...simulation, sim: simulation.empirical };
      document.getElementById("resultProbability").textContent = formatPercent(p * 100);
      document.getElementById("resultProbabilitySource").textContent = mode.value === "observed" ? `${study.onTime} / ${study.known} ${t("common.known").toLowerCase()}` : t("lab.manual");
      document.getElementById("resultGroupSize").textContent = formatNumber(n);
      document.getElementById("resultEventShare").textContent = formatPercent(simulation.simulatedTail * 100);
      document.getElementById("runsCaption").textContent = `${formatNumber(repetitions)} ${t("lab.runs")}`;
      story.innerHTML = "";
      const hits = simulation.histogram.slice(k).reduce((sum, count) => sum + count, 0);
      [
        [t("lab.selectedEvent"), interpolate(t("lab.eventCondition"), { k: formatNumber(k), n: formatNumber(n) })],
        [t("lab.eventCount"), interpolate(t("lab.eventCountText"), { hits: formatNumber(hits), runs: formatNumber(repetitions) })],
        [t("lab.eventShare"), `<strong>${formatPercent(simulation.simulatedTail * 100)}</strong>`]
      ].forEach(([title, body]) => {
        const article = document.createElement("article");
        article.innerHTML = `<h3>${title}</h3><p>${body}</p>`;
        story.append(article);
      });
      const interpretation = document.createElement("article");
      interpretation.className = "interpretation";
      interpretation.innerHTML = `<h3>${t("lab.interpretation")}</h3><p>${t("lab.frequencyNote")}</p>`;
      story.append(interpretation);
      makeTable(document.getElementById("simulatorExact"), [t("lab.tableX"), t("lab.tableFrequency"), t("lab.tableSimulation"), t("lab.tableThreshold")], simulation.histogram.map((count, x) => [x, formatNumber(count), formatPercent(simulation.empirical[x] * 100), x >= k ? "✓" : ""]), t("lab.chartDistribution"));
      resultPanel.hidden = false;
      activeView = document.querySelector('[data-sim-view][aria-selected="true"]')?.dataset.simView || "distribution";
      renderView();
      renderOneGroup();
    }

    function renderChart() {
      chart = destroyChart(chart);
      groupPanel.hidden = activeView !== "group";
      chartFrame.hidden = activeView === "group";
      if (activeView === "group") {
        document.getElementById("simulatorChartText").textContent = t("lab.chartGroup").replace("{n}", String(results.n));
        return;
      }
      const labels = results.histogram.map((_, value) => String(value));
      const colors = labels.map((_, value) => activeView === "tail" && value >= results.k ? "rgba(208,160,58,.72)" : "rgba(73,191,174,.78)");
      const options = commonChartOptions(t("lab.tableFrequency"), undefined);
      options.plugins.legend.display = false;
      options.plugins.tooltip.callbacks.label = context => `${t("lab.tableFrequency")}: ${formatNumber(context.raw)} (${formatPercent(100 * context.raw / results.repetitions)})`;
      options.scales.x.title = { display: true, text: t("lab.tableX"), color: palette.muted };
      options.scales.x.ticks.autoSkip = true;
      options.scales.x.ticks.maxTicksLimit = 12;
      options.scales.y.ticks.precision = 0;
      const title = t(activeView === "tail" ? "lab.chartTail" : "lab.chartDistribution").replace("{k}", String(results.k));
      document.getElementById("simulatorChartText").textContent = title;
      canvas.setAttribute("aria-label", title);
      chart = new Chart(canvas, {
        type: "bar", data: { labels, datasets: [{ label: t("lab.tableFrequency"), data: results.histogram, backgroundColor: colors, borderColor: colors, borderWidth: 1, borderRadius: 2 }] }, options
      });
    }

    function renderOneGroup() {
      if (!results) return;
      let successes = 0;
      const dots = document.getElementById("experimentDots");
      dots.replaceChildren();
      for (let index = 1; index <= results.n; index += 1) {
        const success = rng() < results.p;
        if (success) successes += 1;
        const dot = document.createElement("span");
        dot.className = `student-dot${success ? " is-success" : ""}`;
        dot.setAttribute("aria-hidden", "true");
        dot.title = t(success ? "lab.studentSuccess" : "lab.studentFailure").replace("{n}", String(index));
        dots.append(dot);
      }
      const groupText = interpolate(t("lab.groupOutcome"), { success: formatNumber(successes), n: formatNumber(results.n) });
      document.getElementById("groupResult").textContent = groupText;
      document.getElementById("groupAccessible").textContent = groupText;
    }

    function renderView() {
      document.querySelectorAll("[data-sim-view]").forEach(button => button.setAttribute("aria-selected", String(button.dataset.simView === activeView)));
      if (results) renderChart();
    }

    runButton.addEventListener("click", runExperiment);
    document.querySelectorAll("[data-sim-view]").forEach(button => button.addEventListener("click", () => {
      activeView = button.dataset.simView;
      renderView();
      if (results) renderOneGroup();
    }));
    document.getElementById("anotherGroup").addEventListener("click", renderOneGroup);
    document.getElementById("threshold").addEventListener("change", () => { if (results) runExperiment(); });
  }

  initOverview();
  initData();
  initSimulator();
})();
