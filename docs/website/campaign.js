/* Launch-story copy and guided, screen-by-screen product workflows. All examples are illustrative. */
window.campaignCopy = {
  en: {
    navFeatures:'Features',navTour:'App tour',navCustomization:'Customization',navHow:'How it works',navPrivacy:'Security & privacy',playAd:'Play the launch story',
    uspEyebrow:'Built around your requirements',uspTitle:'A strong starting point.<br><em>Your needs shape what comes next.</em>',uspIntro:'Start with the farm tools that matter today. Discuss the workflow you need; agree the scope before any tailored feature is promised.',securityNote:'These controls are useful safeguards, not a promise of absolute security. Keep the device updated and protect backup files.',workflowAria:'Guided workflows',adProgressLabel:'Launch story progress',
    uspCards:[
      ['✳','Made to fit your workflow','Choose the functions you need. Share the real task behind them; requested changes can be scoped, agreed and built for your use case.','CUSTOMIZABLE'],
      ['▣','Practical security safeguards','PIN access, optional fingerprint unlock, auto-lock after more than 30 seconds in the background, and release-build screenshot/recents protection. PIN material is stored with encrypted preferences and a salted hash.','SECURITY BY DESIGN'],
      ['▤','Seven built-in PDF reports','Generate Gujarati A4 PDFs for daily ledger, farm P&L, Vigha scorecard, manager advances, personal account, pending credit, and assets/loans. PDFs can be opened or shared from the app.','GENERATE · SHARE · PRINT'],
      ['◉','Choose the features you need','Begin with the relevant modules, then discuss additions. Report types or workflows beyond the current app can be defined as a separate customization request.','YOUR FEATURE SET']
    ],
    builderKicker:'MAKE IT YOURS',builderTitle:'Choose the features you need.',builderIntro:'Try the selector to shape a sample feature set. This preview stays in your browser; it is not sent as a request.',builderSelected:'features selected',copyWishlist:'Copy my feature list',builderPrivacy:'No data leaves this page.',builderEmpty:'Select capabilities to create a discussion list.',builderSummary:'Selected for discussion: {list}. This stays in this browser; no request was sent.',copied:'Feature list copied.',copyUnavailable:'Your feature list is ready below; select and copy it manually.',
    builderOptions:['Farm accounts & seasonal P&L','Milk register & dairy comparison','Manager advances','Shopping, assets & lending','Panchang & festival alerts','Reminders & home widgets','PDF reports','PIN & fingerprint safeguards','Another workflow or report (custom request)'],
    processEyebrow:'A CLEAR CUSTOMIZATION PATH',processTitle:'From your need to a considered build.',processSteps:[
      ['01','Describe the task','Tell us what you need to keep track of and how the work happens today.'],
      ['02','Choose a starting set','Select the existing features that fit; mark what is missing or needs to work differently.'],
      ['03','Agree the scope','Review the requested behaviour, screens and report needs together before development begins.'],
      ['04','Build and review','Requested features can then be implemented to the agreed scope and reviewed with you.']
    ],
    workflowEyebrow:'THE EVERYDAY FLOW',workflowTitle:'Step by step,<br><em>screen by screen.</em>',workflowIntro:'Follow a task from its first screen to a useful outcome. Pick a flow; open any step in the interactive screen tour.',showScreen:'Open this screen',
    flows:[
      {id:'entry',title:'Record an entry',description:'A practical path from a quick income or expense to a saved, reviewable record.',steps:[
        ['Start at Home','Choose the income or expense action from the dashboard.','home'],
        ['Choose the account section','Select farm, animal, manager advances, general costs or personal.','entry'],
        ['Add the details','Pick the farm/category and enter the amount. Use voice, a bill photo or the Mann calculator when relevant.','entry'],
        ['Set payment status','Mark paid, or record pending credit with the relevant person’s name.','entry'],
        ['Save and review','Save the record, then review it in the relevant account or farm view.','entry']
      ]},
      {id:'season',title:'Review a farm season',description:'Move from the farm list to seasonal income, costs and the per-Vigha view.',steps:[
        ['Open the farm list','See configured farms and the active crop summary.','farms'],
        ['Choose a farm and season','Open the farm detail for the period you want to review.','farmDetail'],
        ['Read the result','Compare income, expenses and net result; the scorecard ranks net result per Vigha.','farmDetail'],
        ['Create a report','Choose the farm P&L or Vigha scorecard PDF and its supported period.','reports']
      ]},
      {id:'lending',title:'Lend and track an asset',description:'Keep the handover and expected return visible without sending a message automatically.',steps:[
        ['Check the asset register','Choose a registered item or add an asset first.','assets'],
        ['Record the handover','Add borrower, date, expected return and optional photo.','lend'],
        ['Review the prepared message','Open a pre-filled WhatsApp message to review; the app does not send it automatically.','lend'],
        ['Follow the return','See lent/overdue status and use the return reminder actions.','reminders']
      ]},
      {id:'reports',title:'Generate and share a PDF',description:'Choose one of the current report types, generate the file, then share or print it from the app.',steps:[
        ['Open PDF reports','Choose from seven built-in report types.','reports'],
        ['Set the period','Use a date range for reports that support it; pending credit and assets/loans are current-list reports.','reports'],
        ['Generate the Gujarati PDF','Create a paginated A4 PDF with the report content.','reports'],
        ['Open or share','Use Android’s open/share chooser to open or share the PDF; printing depends on the installed PDF viewer.','reports']
      ]},
      {id:'milk',title:'Log milk and compare payment',description:'Record animal-wise milk details, then compare the expected dairy amount with the payment received.',steps:[
        ['Open the milk register','Choose the date and open the register.','milk'],
        ['Record each milking','Select or enter the animal, morning/evening, litres, fat and rate.','milk'],
        ['Review the comparison','Check expected dairy payment, received amount and difference.','milk'],
        ['Record dairy payment','When payment arrives, add it as animal-husbandry income.','entry']
      ]},
      {id:'security',title:'Protect access to the app',description:'Use the available access controls; device support determines whether biometric unlock is available.',steps:[
        ['Set a four-digit PIN','Create and confirm a PIN during first setup.','pinSetup'],
        ['Enable fingerprint if supported','Turn on biometric unlock in security settings when the device offers it.','security'],
        ['Lock on return','The app locks again after it has been in the background for over 30 seconds.','lock'],
        ['Keep the device protected','Release builds block screenshots and recent-app previews; keep your device and backup files secure.','security']
      ]}
    ],
    adLabel:'FARM LEDGER · LAUNCH STORY',adDuration:'A 30-second interactive preview',adTitle:'A season has many stories.<br><em>Keep the useful parts together.</em>',adPause:'Pause',adResume:'Resume',adReplay:'Replay the story',adDisclosure:'Illustrative screens · no live data · no audio',adVisualCaption:'A feature-led product story',adCustomLink:'Want a different feature set? Explore customization',adClose:'Close launch ad',adPrevious:'Previous scene',adNext:'Next scene',adPaused:'Paused',adScenes:[
      'A quick overview brings farm activity and recent records together.',
      'Capture an income or expense with notes, bill photo, voice or Mann calculation.',
      'Review a farm’s season and net result per Vigha.',
      'Keep assets, handovers and return reminders in view.',
      'Create one of seven Gujarati PDF reports, then open or share it.',
      'Use PIN and optional fingerprint safeguards — and shape future features around agreed needs.'
    ]
  },
  gu: {
    navFeatures:'સુવિધાઓ',navTour:'એપની મુલાકાત',navCustomization:'તમારી જરૂર મુજબ',navHow:'કેવી રીતે વાપરવું',navPrivacy:'સુરક્ષા અને ગોપનીયતા',playAd:'લૉન્ચ વાર્તા જુઓ',
    uspEyebrow:'તમારી જરૂરિયાત પ્રમાણે રચાયેલું',uspTitle:'મજબૂત શરૂઆત.<br><em>આગળ શું ઉમેરવું તે તમારી જરૂર નક્કી કરે.</em>',uspIntro:'આજે ઉપયોગી ખેતીનાં સાધનોથી શરૂ કરો. જરૂરી કામની રીત અંગે ચર્ચા કરો; ખાસ સુવિધા ઉમેરવાનું વચન આપતાં પહેલાં તેનો વ્યાપ નક્કી કરો.',securityNote:'આ વ્યવસ્થાઓ ઉપયોગી સુરક્ષા છે, સંપૂર્ણ સુરક્ષાની ખાતરી નહીં. ફોન અપડેટ રાખો અને બેકઅપ ફાઇલ સુરક્ષિત રાખો.',workflowAria:'માર્ગદર્શિત કાર્યપ્રવાહ',adProgressLabel:'લૉન્ચ વાર્તાની પ્રગતિ',
    uspCards:[
      ['✳','તમારી કામની રીત પ્રમાણે','જરૂરી સુવિધા પસંદ કરો અને તેની પાછળનું વાસ્તવિક કામ જણાવો. વિનંતી થયેલા ફેરફારનો વ્યાપ નક્કી કરી સંમતિ પછી અમલમાં મૂકી શકાય.','જરૂર મુજબ ફેરફાર'],
      ['▣','વ્યવહારુ સુરક્ષા વ્યવસ્થા','PINથી પ્રવેશ, વૈકલ્પિક ફિંગરપ્રિન્ટ, એપ ૩૦ સેકન્ડથી વધુ પૃષ્ઠભૂમિમાં રહે પછી લોક, અને રિલીઝ આવૃત્તિમાં સ્ક્રીનશૉટ/રીસેન્ટ્સ સુરક્ષા. PINની માહિતી એન્ક્રિપ્ટેડ પસંદગીમાં સોલ્ટેડ હૅશ રૂપે સચવાય છે.','સુરક્ષા ધ્યાનમાં રાખીને'],
      ['▤','સાત તૈયાર PDF રિપોર્ટ','દૈનિક હિસાબ, ખેતર P&L, વીઘા સ્કોરકાર્ડ, ભાગિયાના ઉપાડ, અંગત હિસાબ, ઉધાર બાકી અને મિલકત/ઉછીના માટે ગુજરાતી A4 PDF. એપમાંથી PDF ખોલી કે વહેંચી શકાય છે.','બનાવો · વહેંચો · છાપો'],
      ['◉','જરૂરી સુવિધા જ પસંદ કરો','યોગ્ય વિભાગોથી શરૂઆત કરો અને વધારાની જરૂરિયાત અંગે ચર્ચા કરો. હાલની એપની બહારનો રિપોર્ટ કે કાર્યપ્રવાહ અલગ કસ્ટમ વિનંતી તરીકે નક્કી કરી શકાય.','તમારી પસંદગી'],
    ],
    builderKicker:'તમારી જરૂર પ્રમાણે',builderTitle:'તમને જરૂરી સુવિધા પસંદ કરો.',builderIntro:'નમૂનાની સુવિધા યાદી ગોઠવવા પસંદગી કરો. આ પસંદગી આ બ્રાઉઝરમાં જ રહે છે; કોઈ વિનંતી મોકલાતી નથી.',builderSelected:'સુવિધા પસંદ',copyWishlist:'મારી યાદી કૉપી કરો',builderPrivacy:'આ પેજની બહાર કોઈ માહિતી જતી નથી.',builderEmpty:'ચર્ચા માટેની યાદી બનાવવા સુવિધા પસંદ કરો.',builderSummary:'ચર્ચા માટે પસંદ કરેલી સુવિધા: {list}. આ યાદી આ બ્રાઉઝરમાં જ છે; કોઈ વિનંતી મોકલાઈ નથી.',copied:'સુવિધાની યાદી કૉપી થઈ.',copyUnavailable:'તમારી યાદી નીચે તૈયાર છે; તેને પસંદ કરી જાતે કૉપી કરો.',
    builderOptions:['ખેતી હિસાબ અને મોસમનો P&L','દૂધ રજીસ્ટર અને ડેરી સરખામણી','ભાગિયાના ઉપાડ','ખરીદી, મિલકત અને ઉછીની','પંચાંગ અને તહેવાર સૂચના','યાદ અપાવનાર અને હોમ વિજેટ','PDF રિપોર્ટ','PIN અને ફિંગરપ્રિન્ટ સુરક્ષા','બીજી કાર્યપદ્ધતિ કે રિપોર્ટ (કસ્ટમ વિનંતી)'],
    processEyebrow:'કસ્ટમાઇઝેશનનાં સ્પષ્ટ પગલાં',processTitle:'તમારી જરૂરથી વિચારપૂર્વકના અમલ સુધી.',processSteps:[
      ['૦૧','કામની જરૂર જણાવો','હાલમાં શું નોંધવું છે અને કામ કેવી રીતે થાય છે તે સમજાવો.'],
      ['૦૨','શરૂઆતની સુવિધા પસંદ કરો','હાલની એપમાંથી યોગ્ય સુવિધા પસંદ કરો; શું ખૂટે છે કે અલગ રીતે કામ કરવું જોઈએ તે જણાવો.'],
      ['૦૩','વ્યાપ નક્કી કરો','વિકાસ શરૂ કરતાં પહેલાં જરૂરી વર્તન, સ્ક્રીન અને રિપોર્ટ અંગે સાથે મળીને સંમતિ કરો.'],
      ['૦૪','બનાવો અને તપાસો','પછી સંમત વ્યાપ પ્રમાણે માંગેલી સુવિધા અમલમાં મૂકી તમારી સાથે તપાસી શકાય.']
    ],
    workflowEyebrow:'રોજિંદા ઉપયોગનો પ્રવાહ',workflowTitle:'પગલું દર પગલું,<br><em>સ્ક્રીન દર સ્ક્રીન.</em>',workflowIntro:'કાર્યની શરૂઆતથી ઉપયોગી પરિણામ સુધીનો માર્ગ જુઓ. પ્રવાહ પસંદ કરો; તેની કોઈપણ સ્ક્રીન ઇન્ટરેક્ટિવ મુલાકાતમાં ખોલો.',showScreen:'આ સ્ક્રીન ખોલો',
    flows:[
      {id:'entry',title:'એન્ટ્રી નોંધો',description:'ઝડપી આવક કે ખર્ચથી સાચવેલી અને તપાસી શકાય તેવી નોંધ સુધી.',steps:[
        ['હોમથી શરૂ કરો','ડેશબોર્ડ પર આવક કે ખર્ચની ક્રિયા પસંદ કરો.','home'],
        ['હિસાબ વિભાગ પસંદ કરો','ખેતર, પશુપાલન, ભાગિયાનો ઉપાડ, સામાન્ય ખર્ચ કે અંગત પસંદ કરો.','entry'],
        ['વિગતો ઉમેરો','ખેતર/પ્રકાર પસંદ કરી રકમ લખો. જરૂર મુજબ અવાજ, બિલનો ફોટો કે મણ ગણક વાપરો.','entry'],
        ['ચુકવણી સ્થિતિ પસંદ કરો','ચૂકવ્યું પસંદ કરો અથવા સંબંધિત વ્યક્તિના નામ સાથે ઉધાર નોંધો.','entry'],
        ['સાચવો અને તપાસો','નોંધ સાચવી સંબંધિત હિસાબ કે ખેતરની સ્ક્રીનમાં તપાસો.','entry']
      ]},
      {id:'season',title:'ખેતરની મોસમ તપાસો',description:'ખેતરની યાદીથી મોસમની આવક, ખર્ચ અને વીઘા દીઠ પરિણામ સુધી.',steps:[
        ['ખેતરની યાદી ખોલો','ગોઠવેલાં ખેતર અને ચાલુ પાકનો સરવાળો જુઓ.','farms'],
        ['ખેતર અને મોસમ પસંદ કરો','તપાસવાના સમયગાળાનું ખેતર વિગત ખોલો.','farmDetail'],
        ['પરિણામ જુઓ','આવક, ખર્ચ અને ચોખ્ખો સરવાળો સરખાવો; સ્કોરકાર્ડ વીઘા દીઠ ચોખ્ખા પરિણામની ગોઠવણી કરે છે.','farmDetail'],
        ['રિપોર્ટ બનાવો','ખેતર P&L અથવા વીઘા સ્કોરકાર્ડનો PDF અને લાગુ સમયગાળો પસંદ કરો.','reports']
      ]},
      {id:'lending',title:'મિલકત ઉછીની આપી ટ્રૅક કરો',description:'આપેલી વસ્તુ અને પરત તારીખ યાદ રાખો; સંદેશ આપમેળે મોકલાતો નથી.',steps:[
        ['મિલકત યાદી તપાસો','નોંધેલી વસ્તુ પસંદ કરો અથવા પહેલાં નવી મિલકત ઉમેરો.','assets'],
        ['આપ્યાની વિગત નોંધો','લેનાર, આપ્યાની તારીખ, પરત તારીખ અને વૈકલ્પિક ફોટો ઉમેરો.','lend'],
        ['તૈયાર સંદેશ તપાસો','પહેલેથી લખેલો WhatsApp સંદેશ તપાસવા ખોલો; એપ આપમેળે મોકલતી નથી.','lend'],
        ['પરત આવવાની યાદ રાખો','ઉછીની/મોડું સ્થિતિ જુઓ અને પરત યાદ અપાવવાની ક્રિયા વાપરો.','reminders']
      ]},
      {id:'reports',title:'PDF બનાવો અને વહેંચો',description:'હાલના રિપોર્ટમાંથી પસંદ કરો, PDF બનાવો અને એપમાંથી વહેંચો કે છાપો.',steps:[
        ['PDF રિપોર્ટ ખોલો','સાત તૈયાર રિપોર્ટમાંથી પસંદ કરો.','reports'],
        ['સમયગાળો પસંદ કરો','સમયગાળો લાગુ પડતા રિપોર્ટ માટે તારીખ પસંદ કરો; ઉધાર અને મિલકત/ઉછીના વર્તમાન યાદી બતાવે છે.','reports'],
        ['ગુજરાતી PDF બનાવો','રિપોર્ટની વિગતો સાથે પાનાં પ્રમાણે ગોઠવાયેલ A4 PDF બનાવો.','reports'],
        ['ખોલો કે વહેંચો','PDF જોવા કે વહેંચવા Android વિકલ્પ પસંદ કરો; છાપવાની સુવિધા ઇન્સ્ટોલ કરેલા PDF viewer પર આધારિત છે.','reports']
      ]},
      {id:'milk',title:'દૂધ નોંધો અને ચુકવણી સરખાવો',description:'પશુ પ્રમાણે દૂધની વિગતો નોંધો; પછી ડેરીએ આપવાના અને મળેલા પૈસા સરખાવો.',steps:[
        ['દૂધ રજીસ્ટર ખોલો','તારીખ પસંદ કરી રજીસ્ટર ખોલો.','milk'],
        ['દરેક દૂધની વિગત નોંધો','પશુ, સવાર/સાંજ, લિટર, ફેટ અને ભાવ પસંદ કરો કે લખો.','milk'],
        ['સરખામણી જુઓ','ડેરીએ આપવાના, મળેલા પૈસા અને ફરક તપાસો.','milk'],
        ['ડેરીની ચુકવણી નોંધો','પૈસા મળે ત્યારે પશુપાલનની આવક તરીકે નોંધો.','entry']
      ]},
      {id:'security',title:'એપનો પ્રવેશ સુરક્ષિત રાખો',description:'ઉપલબ્ધ સુરક્ષા વાપરો; ફિંગરપ્રિન્ટ ફોનના આધાર પર ઉપલબ્ધ હોય છે.',steps:[
        ['ચાર અંકનો PIN ગોઠવો','પહેલી ગોઠવણીમાં PIN બનાવો અને ખાતરી કરો.','pinSetup'],
        ['સુવિધા હોય તો ફિંગરપ્રિન્ટ ચાલુ કરો','ફોનમાં વિકલ્પ હોય ત્યારે સુરક્ષા સેટિંગમાં ફિંગરપ્રિન્ટ ચાલુ કરો.','security'],
        ['ફરી ખોલતાં લોક','એપ ૩૦ સેકન્ડથી વધુ પૃષ્ઠભૂમિમાં રહે પછી ફરી લોક થાય છે.','lock'],
        ['ફોન સુરક્ષિત રાખો','રિલીઝ આવૃત્તિમાં સ્ક્રીનશૉટ અને રીસેન્ટ્સ પ્રિવ્યૂ બંધ છે; ફોન અને બેકઅપ ફાઇલ સુરક્ષિત રાખો.','security']
      ]}
    ],
    adLabel:'ખેતી હિસાબ · લૉન્ચ વાર્તા',adDuration:'૩૦ સેકન્ડનું ઇન્ટરેક્ટિવ પ્રદર્શન',adTitle:'મોસમની ઘણી વાતો હોય છે.<br><em>ઉપયોગી હિસાબ એકસાથે રાખો.</em>',adPause:'રોકો',adResume:'ચાલુ કરો',adReplay:'વાર્તા ફરી જુઓ',adDisclosure:'નમૂનાની સ્ક્રીન · જીવંત ડેટા નથી · અવાજ નથી',adVisualCaption:'સુવિધા આધારિત ઉત્પાદન ઝાંખી',adCustomLink:'બીજી સુવિધા જોઈએ? કસ્ટમાઇઝેશન જુઓ',adClose:'લૉન્ચ જાહેરાત બંધ કરો',adPrevious:'પહેલાનું દૃશ્ય',adNext:'આગળનું દૃશ્ય',adPaused:'રોકાયેલું',adScenes:[
      'એક ઝાંખીમાં ખેતીની પ્રવૃત્તિ અને તાજેતરની નોંધો જુઓ.',
      'આવક કે ખર્ચ નોંધો; નોંધ, બિલ ફોટો, અવાજ અથવા મણ ગણક ઉમેરો.',
      'ખેતરની મોસમ અને વીઘા દીઠ ચોખ્ખું પરિણામ તપાસો.',
      'મિલકત, આપ્યાની તારીખ અને પરત યાદ અપાવનાર નજરમાં રાખો.',
      'સાત ગુજરાતી PDF રિપોર્ટમાંથી બનાવો અને પછી ખોલો કે વહેંચો.',
      'PIN અને વૈકલ્પિક ફિંગરપ્રિન્ટ વાપરો — અને સંમત જરૂર મુજબ આગળની સુવિધા ગોઠવો.'
    ]
  },
  hi: {
    navFeatures:'सुविधाएँ',navTour:'ऐप परिचय',navCustomization:'ज़रूरत के अनुसार',navHow:'कैसे काम करता है',navPrivacy:'सुरक्षा और गोपनीयता',playAd:'लॉन्च कहानी चलाएँ',
    uspEyebrow:'आपकी ज़रूरत के अनुसार',uspTitle:'एक मज़बूत शुरुआत।<br><em>आगे की दिशा आपकी ज़रूरत तय करे.</em>',uspIntro:'आज काम आने वाले खेती के औज़ारों से शुरू करें। ज़रूरी कार्यप्रणाली पर चर्चा करें; कोई विशेष सुविधा देने का वादा करने से पहले उसका दायरा तय करें।',securityNote:'ये उपयोगी सुरक्षा उपाय हैं, पूर्ण सुरक्षा का वादा नहीं। डिवाइस अपडेट रखें और बैकअप फ़ाइलों को सुरक्षित रखें.',workflowAria:'निर्देशित कार्यप्रवाह',adProgressLabel:'लॉन्च कहानी की प्रगति',
    uspCards:[
      ['✳','आपके काम के अनुसार','ज़रूरी सुविधाएँ चुनें और उनके पीछे का असली काम बताएँ। माँगे गए बदलाव का दायरा तय करके सहमति के बाद बनाया जा सकता है।','ज़रूरत के अनुसार'],
      ['▣','व्यावहारिक सुरक्षा उपाय','PIN से प्रवेश, वैकल्पिक फिंगरप्रिंट, ऐप 30 सेकंड से अधिक बैकग्राउंड में रहने पर ऑटो-लॉक, और रिलीज़ बिल्ड में स्क्रीनशॉट/हाल के ऐप की सुरक्षा। PIN जानकारी एन्क्रिप्टेड प्रेफ़रेंस में salted hash के रूप में रखी जाती है।','सुरक्षा को ध्यान में रखकर'],
      ['▤','सात पहले से उपलब्ध PDF रिपोर्ट','दैनिक हिसाब, खेत P&L, विघा स्कोरकार्ड, प्रबंधक की पेशगी, निजी खाता, बाकी उधार और संपत्ति/उधार के लिए गुजराती A4 PDF। ऐप से PDF खोला या साझा किया जा सकता है।','बनाएँ · साझा करें · प्रिंट करें'],
      ['◉','ज़रूरी सुविधाएँ ही चुनें','काम के मॉड्यूल से शुरू करें, फिर अतिरिक्त ज़रूरत पर चर्चा करें। मौजूदा ऐप से बाहर की रिपोर्ट या कार्यप्रणाली अलग कस्टम अनुरोध के रूप में तय की जा सकती है।','आपकी सुविधा सूची'],
    ],
    builderKicker:'अपनी ज़रूरत के अनुसार',builderTitle:'ज़रूरी सुविधाएँ चुनें.',builderIntro:'नमूने की सुविधा सूची बनाने के लिए चुनें। यह चयन इसी ब्राउज़र में रहता है; अनुरोध के रूप में कहीं भेजा नहीं जाता.',builderSelected:'सुविधाएँ चुनीं',copyWishlist:'मेरी सुविधा सूची कॉपी करें',builderPrivacy:'इस पेज से कोई डेटा बाहर नहीं जाता.',builderEmpty:'चर्चा की सूची बनाने के लिए सुविधाएँ चुनें.',builderSummary:'चर्चा के लिए चुनी गई सुविधाएँ: {list}. यह सूची इसी ब्राउज़र में है; कोई अनुरोध नहीं भेजा गया.',copied:'सुविधा सूची कॉपी हुई.',copyUnavailable:'आपकी सूची नीचे तैयार है; उसे चुनकर स्वयं कॉपी करें.',
    builderOptions:['खेती खाते और मौसम का P&L','दूध रजिस्टर और डेयरी तुलना','प्रबंधक की पेशगी','खरीदारी, संपत्ति और उधार','पंचांग और त्योहार अलर्ट','रिमाइंडर और होम विजेट','PDF रिपोर्ट','PIN और फिंगरप्रिंट सुरक्षा','दूसरी कार्यप्रणाली या रिपोर्ट (कस्टम अनुरोध)'],
    processEyebrow:'कस्टम बनाने के स्पष्ट चरण',processTitle:'ज़रूरत से सोच-समझकर बने समाधान तक.',processSteps:[
      ['०१','काम की ज़रूरत बताएँ','आज क्या दर्ज करना है और काम कैसे होता है, यह साझा करें.'],
      ['०२','शुरुआती सुविधाएँ चुनें','मौजूदा ऐप की काम की सुविधाएँ चुनें; बताएँ कि क्या कम है या अलग तरह से काम करना चाहिए.'],
      ['०३','दायरा तय करें','विकास शुरू होने से पहले ज़रूरी व्यवहार, स्क्रीन और रिपोर्ट पर मिलकर सहमति करें.'],
      ['०४','बनाएँ और जाँचें','फिर सहमत दायरे के अनुसार माँगी गई सुविधा बनाई और आपके साथ जाँची जा सकती है.']
    ],
    workflowEyebrow:'रोज़मर्रा का तरीका',workflowTitle:'कदम दर कदम,<br><em>स्क्रीन दर स्क्रीन.</em>',workflowIntro:'किसी काम की शुरुआत से उपयोगी नतीजे तक का रास्ता देखें। तरीका चुनें; उसका कोई भी चरण इंटरैक्टिव स्क्रीन परिचय में खोलें.',showScreen:'यह स्क्रीन खोलें',
    flows:[
      {id:'entry',title:'एंट्री दर्ज करें',description:'त्वरित आमदनी या खर्च से सहेजे और जाँचे जा सकने वाले रिकॉर्ड तक.',steps:[
        ['होम से शुरू करें','डैशबोर्ड पर आमदनी या खर्च की कार्रवाई चुनें.','home'],
        ['खाता विभाग चुनें','खेत, पशुपालन, प्रबंधक की पेशगी, सामान्य खर्च या निजी खाता चुनें.','entry'],
        ['जानकारी भरें','खेत/श्रेणी और रकम दर्ज करें. ज़रूरत पर आवाज़, बिल की तस्वीर या मन कैलकुलेटर इस्तेमाल करें.','entry'],
        ['भुगतान स्थिति चुनें','भुगतान हुआ चुनें या संबंधित व्यक्ति के नाम के साथ उधार दर्ज करें.','entry'],
        ['सहेजें और जाँचें','रिकॉर्ड सहेजें, फिर संबंधित खाते या खेत की स्क्रीन में जाँचें.','entry']
      ]},
      {id:'season',title:'खेत का मौसम देखें',description:'खेतों की सूची से मौसम की आमदनी, खर्च और प्रति विघा नतीजे तक.',steps:[
        ['खेतों की सूची खोलें','तय खेत और सक्रिय फसल का सार देखें.','farms'],
        ['खेत और मौसम चुनें','जिस अवधि की समीक्षा करनी है, उसका खेत विवरण खोलें.','farmDetail'],
        ['नतीजा देखें','आमदनी, खर्च और शुद्ध नतीजे की तुलना करें; स्कोरकार्ड प्रति विघा शुद्ध नतीजे की रैंकिंग करता है.','farmDetail'],
        ['रिपोर्ट बनाएँ','खेत P&L या विघा स्कोरकार्ड PDF और लागू अवधि चुनें.','reports']
      ]},
      {id:'lending',title:'संपत्ति उधार देकर ट्रैक करें',description:'दी गई वस्तु और लौटाने की तारीख याद रखें; संदेश अपने-आप नहीं भेजा जाता.',steps:[
        ['संपत्ति सूची देखें','दर्ज वस्तु चुनें या पहले नई संपत्ति जोड़ें.','assets'],
        ['देने का विवरण दर्ज करें','लेने वाला, देने की तारीख, लौटाने की अपेक्षित तारीख और वैकल्पिक फोटो जोड़ें.','lend'],
        ['तैयार संदेश जाँचें','पहले से भरा WhatsApp संदेश जाँचने के लिए खोलें; ऐप इसे अपने-आप नहीं भेजता.','lend'],
        ['वापसी पर नज़र रखें','उधार/देरी की स्थिति देखें और वापसी रिमाइंडर की कार्रवाई इस्तेमाल करें.','reminders']
      ]},
      {id:'reports',title:'PDF बनाएँ और साझा करें',description:'मौजूदा रिपोर्ट में से चुनें, PDF बनाएँ, फिर ऐप से साझा या प्रिंट करें.',steps:[
        ['PDF रिपोर्ट खोलें','सात उपलब्ध रिपोर्ट में से चुनें.','reports'],
        ['अवधि तय करें','जहाँ रिपोर्ट में अवधि लागू है, वहाँ तारीख चुनें; बाकी उधार और संपत्ति/उधार मौजूदा सूची दिखाते हैं.','reports'],
        ['गुजराती PDF बनाएँ','रिपोर्ट के विवरण के साथ पृष्ठों में व्यवस्थित A4 PDF बनाएँ.','reports'],
        ['खोलें या साझा करें','PDF खोलने या साझा करने के लिए Android विकल्प चुनें; प्रिंट की सुविधा इंस्टॉल किए PDF viewer पर निर्भर है.','reports']
      ]},
      {id:'milk',title:'दूध दर्ज करें और भुगतान तुलना करें',description:'पशु के हिसाब से दूध की जानकारी दर्ज करें, फिर डेयरी की देय और मिली रकम की तुलना करें.',steps:[
        ['दूध रजिस्टर खोलें','तारीख चुनकर रजिस्टर खोलें.','milk'],
        ['हर दुहाई का विवरण दर्ज करें','पशु, सुबह/शाम, लीटर, फैट और दर चुनें या भरें.','milk'],
        ['तुलना देखें','डेयरी की देय रकम, मिली रकम और अंतर जाँचें.','milk'],
        ['डेयरी भुगतान दर्ज करें','भुगतान मिलने पर उसे पशुपालन की आमदनी के रूप में दर्ज करें.','entry']
      ]},
      {id:'security',title:'ऐप का प्रवेश सुरक्षित रखें',description:'उपलब्ध सुरक्षा नियंत्रण अपनाएँ; फिंगरप्रिंट की उपलब्धता डिवाइस पर निर्भर है.',steps:[
        ['चार अंकों का PIN सेट करें','पहली सेटिंग में PIN बनाएँ और उसकी पुष्टि करें.','pinSetup'],
        ['उपलब्ध हो तो फिंगरप्रिंट चालू करें','डिवाइस में सुविधा हो तो सुरक्षा सेटिंग में बायोमेट्रिक चालू करें.','security'],
        ['लौटने पर लॉक','ऐप 30 सेकंड से अधिक बैकग्राउंड में रहने के बाद फिर लॉक होता है.','lock'],
        ['डिवाइस सुरक्षित रखें','रिलीज़ बिल्ड स्क्रीनशॉट और हाल के ऐप प्रीव्यू रोकता है; डिवाइस और बैकअप फ़ाइलें सुरक्षित रखें.','security']
      ]}
    ],
    adLabel:'खेती का हिसाब · लॉन्च कहानी',adDuration:'30 सेकंड का इंटरैक्टिव परिचय',adTitle:'मौसम में कई कहानियाँ होती हैं.<br><em>ज़रूरी हिसाब साथ रखें.</em>',adPause:'रोकें',adResume:'जारी रखें',adReplay:'कहानी फिर चलाएँ',adDisclosure:'नमूना स्क्रीन · लाइव डेटा नहीं · ऑडियो नहीं',adVisualCaption:'सुविधा-केंद्रित उत्पाद परिचय',adCustomLink:'दूसरी सुविधा चाहिए? कस्टमाइज़ेशन देखें',adClose:'लॉन्च विज्ञापन बंद करें',adPrevious:'पिछला दृश्य',adNext:'अगला दृश्य',adPaused:'रुका हुआ',adScenes:[
      'एक नज़र में खेती की गतिविधि और हाल के रिकॉर्ड देखें.',
      'आमदनी या खर्च दर्ज करें; नोट, बिल की तस्वीर, आवाज़ या मन गणना जोड़ें.',
      'खेत का मौसम और प्रति विघा शुद्ध नतीजा देखें.',
      'संपत्ति, देने की तारीख और वापसी रिमाइंडर पर नज़र रखें.',
      'सात गुजराती PDF रिपोर्ट में से बनाएँ, फिर खोलें या साझा करें.',
      'PIN और वैकल्पिक फिंगरप्रिंट सुरक्षा अपनाएँ — और सहमत ज़रूरत के अनुसार अगली सुविधा तय करें.'
    ]
  }
};
