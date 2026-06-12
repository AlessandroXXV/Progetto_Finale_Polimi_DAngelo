// Public-facing landing page targeting clients (buyers). Showcases services, price comparisons, trust signals, and FAQ.
import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Sparkles,
  GraduationCap,
  Palette,
  Globe,
  Laptop,
  Smartphone,
  Users,
  User,
  Briefcase,
  Sofa,
  Store,
  IdCard,
  Lock,
  MessageCircle,
  ArrowRight,
} from 'lucide-react'
import './ClientiLandingPage.css'

// Static FAQ content defined outside the component to avoid re-creation on every render.
const FAQS = [
  {
    q: 'Quanto costano i servizi su Eably?',
    a: 'I prezzi sono fissati direttamente dagli studenti, quindi variano per categoria e professionista. In media i clienti risparmiano il 60% rispetto alle tariffe di agenzie o liberi professionisti tradizionali. Ogni tariffa è visibile in anticipo, senza costi nascosti.',
  },
  {
    q: 'Come faccio a sapere se il professionista è affidabile?',
    a: 'Ogni studente su Eably è verificato con documento d\'identità prima di poter pubblicare servizi. Puoi inoltre vedere le competenze dichiarate, il profilo universitario e — man mano che la community cresce — le recensioni di altri clienti. La trasparenza è il cuore di Eably.',
  },
  {
    q: 'Cosa succede se non sono soddisfatto del servizio?',
    a: 'Il pagamento tramite Stripe viene confermato solo dopo il completamento del servizio. Se il servizio non viene erogato correttamente, puoi aprire una segnalazione e il team Eably valuterà il rimborso. Sei sempre tutelato.',
  },
  {
    q: 'Che servizi posso trovare su Eably?',
    a: 'Ripetizioni per ogni materia e livello scolastico, tutoring universitario, grafica e design, traduzione in multiple lingue, assistenza informatica, gestione social media, copywriting, supporto con documenti digitali, e molto altro. Oltre 24 categorie di servizio, tutte erogate da studenti universitari verificati.',
  },
  {
    q: 'Devo scaricare un\'app?',
    a: 'No. Eably funziona interamente dal browser del tuo smartphone, tablet o computer. Nessuna app da scaricare, nessuna registrazione obbligatoria per sfogliare i servizi. Accedi, scegli, paga. Tutto in pochi minuti.',
  },
  {
    q: 'Posso chiedere un servizio non presente nella lista?',
    a: 'Sì. Puoi pubblicare una richiesta personalizzata e gli studenti della community potranno risponderti con un\'offerta su misura. Se hai un bisogno specifico, c\'è quasi certamente qualcuno su Eably in grado di aiutarti.',
  },
]

// Data for the price-comparison bar chart: each entry defines widths as CSS percentages used via a CSS custom property.
const BARS = [
  { service: 'Ripetizioni', traditional: { w: '82%', label: '€30/ora' }, eably: { w: '40%', label: '€12/ora' }, saving: '−60%' },
  { service: 'Traduzione', traditional: { w: '75%', label: '€60/1k parole' }, eably: { w: '28%', label: '€22/1k parole' }, saving: '−63%' },
  { service: 'Logo design', traditional: { w: '90%', label: '€400/progetto' }, eably: { w: '38%', label: '€150/progetto' }, saving: '−62%' },
]

export default function ClientiLandingPage() {
  const [scrolled, setScrolled] = useState(false)
  // openFaq stores the index of the currently expanded FAQ item, or null if all are collapsed.
  const [openFaq, setOpenFaq] = useState(null)
  // Refs for the two-part custom cursor: a small dot (cursorRef) and a larger lagging ring (ringRef).
  const cursorRef = useRef(null)
  const ringRef = useRef(null)

  // Custom cursor: the dot follows the mouse instantly; the ring lags behind with a lerp factor of 0.15.
  useEffect(() => {
    let mx = 0, my = 0, rx = 0, ry = 0, raf
    const onMove = (e) => { mx = e.clientX; my = e.clientY }
    const anim = () => {
      // Linear interpolation gives the ring a smooth trailing effect.
      rx += (mx - rx) * 0.15
      ry += (my - ry) * 0.15
      if (cursorRef.current) { cursorRef.current.style.left = mx + 'px'; cursorRef.current.style.top = my + 'px' }
      if (ringRef.current) { ringRef.current.style.left = rx + 'px'; ringRef.current.style.top = ry + 'px' }
      raf = requestAnimationFrame(anim)
    }
    document.addEventListener('mousemove', onMove)
    raf = requestAnimationFrame(anim)
    return () => { document.removeEventListener('mousemove', onMove); cancelAnimationFrame(raf) }
  }, [])

  // Nav scroll: adds a 'cl-scrolled' class to the navbar after the user scrolls past 40 px.
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 40)
    window.addEventListener('scroll', onScroll)
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  // Reveal observer: adds 'cl-visible' to elements with 'cl-reveal' when they enter the viewport (10% threshold).
  useEffect(() => {
    const obs = new IntersectionObserver(
      entries => entries.forEach(e => { if (e.isIntersecting) e.target.classList.add('cl-visible') }),
      { threshold: 0.1 }
    )
    document.querySelectorAll('.cl-reveal').forEach(el => obs.observe(el))
    return () => obs.disconnect()
  }, [])

  // Bar animation observer: triggers the CSS width animation on comparison bars once they are 30% visible.
  // Each bar is unobserved after its first trigger to avoid re-animating on scroll back.
  useEffect(() => {
    const barObs = new IntersectionObserver(entries => {
      entries.forEach(e => {
        if (e.isIntersecting) {
          e.target.querySelectorAll('.cl-bar-fill').forEach(b => setTimeout(() => b.classList.add('cl-animated'), 100))
          barObs.unobserve(e.target)
        }
      })
    }, { threshold: 0.3 })
    document.querySelectorAll('.cl-comparison-bar').forEach(el => barObs.observe(el))
    return () => barObs.disconnect()
  }, [])

  // Toggle FAQ accordion: clicking the open item closes it; clicking a different item opens it.
  const toggleFaq = (i) => setOpenFaq(openFaq === i ? null : i)

  return (
    <div className="cl-root">
      {/* Custom cursor elements positioned via JS in the effect above. */}
      <div className="cl-cursor" ref={cursorRef} />
      <div className="cl-cursor-ring" ref={ringRef} />

      {/* NAV */}
      <nav className={`cl-nav${scrolled ? ' cl-scrolled' : ''}`}>
        <Link to="/" className="cl-nav-logo">
          <img src="/logo.png" alt="Eably" className="cl-nav-logo-img" />
        </Link>
        <div className="cl-nav-links">
          <a href="#servizi">Servizi</a>
          <a href="#per-chi">Per chi è</a>
          <a href="#risparmia">Risparmia</a>
          <a href="#faq">FAQ</a>
        </div>
        <Link to="/browse" className="cl-btn-nav">
          Trova un professionista
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M2 7h10M8 3l4 4-4 4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
        </Link>
      </nav>

      {/* HERO */}
      <header className="cl-hero">
        <div className="cl-hero-bg" />
        <div className="cl-hero-left">
          <div className="cl-hero-badge"><Sparkles className="inline-block h-4 w-4 align-text-bottom" strokeWidth={1.5} /> Marketplace verificato · Pagamenti sicuri Stripe</div>
          <h1>
            Il talento
            <em className="cl-block"> dei giovani,</em>
            al tuo servizio.
          </h1>
          <p className="cl-hero-sub">
            Trova ripetizioni, tutoring, grafica, traduzioni e molto altro. Servizi offerti da{' '}
            <strong>studenti universitari verificati</strong>, a prezzi accessibili. Fai qualcosa di buono per te — e per chi studia.
          </p>
          <div className="cl-hero-ctas">
            <Link to="/browse" className="cl-btn-primary">
              Esplora i servizi
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M3 8h10M9 4l4 4-4 4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </Link>
            <a href="#per-chi" className="cl-btn-ghost">Scopri se fa per te</a>
          </div>
          <div className="cl-hero-trust">
            <span className="cl-trust-chip"><span className="cl-trust-chip-dot cl-td-green" />Profili verificati</span>
            <span className="cl-trust-chip"><span className="cl-trust-chip-dot cl-td-purple" />Pagamenti sicuri</span>
            <span className="cl-trust-chip"><span className="cl-trust-chip-dot cl-td-amber" />Zero app da scaricare</span>
          </div>
        </div>

        {/* Service mosaic — also serves as the anchor for the #servizi nav link. */}
        <div className="cl-hero-right" id="servizi">
          <div className="cl-services-mosaic">
            <div className="cl-svc-card cl-featured">
              <span className="cl-svc-icon"><GraduationCap className="h-7 w-7" strokeWidth={1.5} /></span>
              <div className="cl-svc-name">Ripetizioni &amp; Tutoring</div>
              <div className="cl-svc-desc">Matematica, fisica, lingue, materie universitarie. Per bambini, ragazzi e adulti di qualsiasi livello.</div>
              <span className="cl-svc-price">A partire da €12/ora</span>
            </div>
            <div className="cl-svc-card">
              <span className="cl-svc-icon"><Palette className="h-7 w-7" strokeWidth={1.5} /></span>
              <div className="cl-svc-name">Grafica &amp; Design</div>
              <div className="cl-svc-desc">Loghi, social, presentazioni professionali.</div>
              <span className="cl-svc-price">Da €20/progetto</span>
            </div>
            <div className="cl-svc-card">
              <span className="cl-svc-icon"><Globe className="h-7 w-7" strokeWidth={1.5} /></span>
              <div className="cl-svc-name">Traduzione</div>
              <div className="cl-svc-desc">Inglese, francese, spagnolo, tedesco e altro.</div>
              <span className="cl-svc-price">Da €15/1000 parole</span>
            </div>
            <div className="cl-svc-card">
              <span className="cl-svc-icon"><Laptop className="h-7 w-7" strokeWidth={1.5} /></span>
              <div className="cl-svc-name">Assistenza PC</div>
              <div className="cl-svc-desc">Setup, problemi tecnici, basi di informatica.</div>
              <span className="cl-svc-price">Da €18/ora</span>
            </div>
            <div className="cl-svc-card">
              <span className="cl-svc-icon"><Smartphone className="h-7 w-7" strokeWidth={1.5} /></span>
              <div className="cl-svc-name">Social Media</div>
              <div className="cl-svc-desc">Gestione profili, contenuti, crescita organica.</div>
              <span className="cl-svc-price">Da €60/mese</span>
            </div>
            <div className="cl-svc-more">+ <span>24 categorie</span> disponibili</div>
          </div>
        </div>
      </header>

      {/* MISSION */}
      <section className="cl-mission-section cl-reveal">
        <div className="cl-mission-inner">
          <div className="cl-mission-section-label">Più di un marketplace</div>
          <blockquote className="cl-mission-quote">
            "Ogni servizio che acquisti<br />è un <em>futuro che investi.</em>"
          </blockquote>
          <div className="cl-mission-divider" />
          <p className="cl-mission-body">
            Quando scegli Eably non stai solo risparmiando. Stai <strong>sostenendo uno studente</strong> che sta costruendo il suo futuro mentre studia, che ha scelto di non cedere alla precarietà e di valorizzare le proprie competenze.<br /><br />
            È una catena virtuosa: tu ottieni un servizio di qualità a un prezzo onesto, lo studente cresce, acquisisce esperienza reale e si avvicina al mercato del lavoro con qualcosa di concreto. <strong>Tutti ci guadagnano. Davvero.</strong>
          </p>
        </div>
      </section>

      {/* SAVINGS */}
      <section className="cl-savings-section" id="risparmia">
        <div className="cl-section-label">Il confronto che fa la differenza</div>
        <h2>Risparmia fino al <em>60%</em><br />senza rinunciare alla qualità.</h2>
        <p className="cl-sub">Prezzi calcolati su un campione di 20 servizi a confronto tra le tariffe medie di mercato e quelle dei professionisti su Eably.</p>

        {/* Bar chart: each service has two rows (traditional vs Eably). Bar widths animate via CSS once visible. */}
        <div className="cl-comparison-bar cl-reveal">
          <div className="cl-comp-label">Confronto prezzi orari medi · mercato tradizionale vs Eably</div>
          {BARS.map((bar, i) => (
            <div key={i}>
              <div className="cl-bar-row">
                <div className="cl-bar-service">{bar.service}</div>
                <div className="cl-bar-track">
                  {/* --w CSS custom property drives the animated width in the stylesheet. */}
                  <div className="cl-bar-fill cl-traditional" style={{ '--w': bar.traditional.w }}>{bar.traditional.label}</div>
                </div>
                <div className="cl-bar-saving" />
              </div>
              <div className="cl-bar-row">
                <div className="cl-bar-service">{bar.service}</div>
                <div className="cl-bar-track">
                  <div className="cl-bar-fill cl-eably" style={{ '--w': bar.eably.w }}>{bar.eably.label}</div>
                </div>
                <div className="cl-bar-saving">{bar.saving}</div>
              </div>
              {i < BARS.length - 1 && <div style={{ height: '20px' }} />}
            </div>
          ))}
        </div>

        <div className="cl-savings-grid cl-reveal">
          <div className="cl-saving-stat">
            <div className="cl-saving-num">−<span>60%</span></div>
            <div className="cl-saving-desc">Risparmio medio calcolato su 20 categorie di servizio a confronto con agenzie e freelance tradizionali</div>
          </div>
          <div className="cl-saving-stat">
            <div className="cl-saving-num"><span>100%</span></div>
            <div className="cl-saving-desc">Profili verificati con documento. Sai sempre con chi parli, prima ancora di pagare</div>
          </div>
          <div className="cl-saving-stat">
            <div className="cl-saving-num">−<span>70%</span></div>
            <div className="cl-saving-desc">Rischio di truffe rispetto ai canali informali, grazie ai pagamenti protetti Stripe</div>
          </div>
        </div>
      </section>

      {/* FOR WHOM */}
      <section className="cl-whom-section" id="per-chi">
        <div className="cl-section-label cl-reveal">Una piattaforma, mille bisogni</div>
        <h2 className="cl-reveal">Pensato per <em>le persone reali.</em><br />Come te.</h2>
        {/* Persona cards rendered from an inline array; each card has a CSS class for its color accent and an optional stagger delay. */}
        <div className="cl-whom-grid">
          {[
            { cls: 'cl-wc-parents', icon: Users, title: 'Genitori con figli a scuola', body: <>Cerchi ripetizioni per tuo figlio ma i centri studio costano troppo? Su Eably trovi <strong>studenti universitari preparati</strong> che seguono i ragazzi con metodo, empatia e a prezzi che non pesano sul bilancio familiare.</> },
            { cls: 'cl-wc-elderly', icon: User, title: 'Persone anziane o meno digitali', body: <>Lo smartphone è un mistero? Il PC fa paura? Trova un <strong>giovane paziente e disponibile</strong> che ti insegna a usare la tecnologia, ti aiuta con i documenti online o semplicemente ti tiene compagnia con un videoclip. Con calma. Con rispetto.</>, delay: '0.1s' },
            { cls: 'cl-wc-busy', icon: Briefcase, title: 'Coppie e professionisti indaffarati', body: <>Tra lavoro, famiglia e mille impegni non hai tempo per tutto. Delega a un esperto la <strong>gestione social, la grafica per il tuo lavoro, la traduzione di un documento</strong> — e concentrati su ciò che conta davvero.</>, delay: '0.2s' },
            { cls: 'cl-wc-students', icon: GraduationCap, title: 'Studenti universitari', body: <>Sei iscritto all'università ma hai lacune in alcune materie? Trovare un tutor tra pari che <strong>parla la tua lingua e conosce i tuoi manuali</strong> è un vantaggio enorme. Più economico di un professore, più efficace di YouTube.</> },
            { cls: 'cl-wc-singles', icon: Sofa, title: 'Privati con piccoli bisogni', body: <>Hai bisogno di un testo tradotto, una presentazione rifinita, un logo per la tua attività hobbistica? Piccoli lavori che non giustificano un'agenzia, ma che <strong>meritano qualità vera.</strong></>, delay: '0.1s' },
            { cls: 'cl-wc-small-biz', icon: Store, title: 'Piccole imprese e artigiani', body: <>Una serranda, una trattoria, un negozio di quartiere. Hai bisogno di <strong>presenza online, materiali grafici, o qualcuno che gestisca Instagram</strong>? Eably mette i giovani talenti a portata di un click — senza costi di agenzia.</>, delay: '0.2s' },
          ].map(({ cls, icon: Icon, title, body, delay }, i) => (
            <div key={i} className={`cl-whom-card ${cls} cl-reveal`} style={delay ? { transitionDelay: delay } : {}}>
              <span className="cl-whom-emoji"><Icon className="h-7 w-7" strokeWidth={1.5} /></span>
              <div className="cl-whom-title">{title}</div>
              <p className="cl-whom-copy">{body}</p>
            </div>
          ))}
        </div>
      </section>

      {/* HOW IT WORKS */}
      <section className="cl-how-section">
        <div className="cl-section-label cl-reveal">Semplicissimo da usare</div>
        <h2 className="cl-reveal">Dal bisogno al servizio<br /><em>in quattro passi.</em></h2>
        {/* Steps rendered from an inline array with staggered reveal delays. */}
        <div className="cl-steps-row">
          {[
            { n: '1', title: 'Cerca il servizio', text: 'Naviga tra le categorie o usa la barra di ricerca. Trova esattamente quello che ti serve, con filtri per materia, disponibilità e prezzo.' },
            { n: '2', title: 'Scegli il profilo', text: 'Ogni studente ha un profilo verificato con competenze, disponibilità e tariffe chiare. Nessuna sorpresa, nessun margine di incertezza.', delay: '0.12s' },
            { n: '3', title: 'Paga in sicurezza', text: 'Il pagamento avviene online tramite Stripe. I tuoi soldi sono protetti fino al completamento del servizio. Zero rischi.', delay: '0.24s' },
            { n: '4', title: 'Ricevi e valuta', text: 'Ottieni il servizio e lascia una recensione. Costruiamo insieme una community affidabile, dove la qualità è premiata.', delay: '0.36s' },
          ].map(({ n, title, text, delay }, i) => (
            <div key={i} className="cl-step cl-reveal" style={delay ? { transitionDelay: delay } : {}}>
              <div className="cl-step-dot">{n}</div>
              <h3>{title}</h3>
              <p>{text}</p>
            </div>
          ))}
        </div>
      </section>

      {/* TRUST */}
      <section className="cl-trust-section">
        <div className="cl-section-label cl-reveal">Sicurezza prima di tutto</div>
        <h2 className="cl-reveal">Ogni dettaglio è <em>verificato.</em><br />Così puoi stare tranquillo.</h2>
        <div className="cl-trust-grid">
          {[
            { icon: IdCard, title: 'Identità verificata', text: 'Ogni studente carica un documento d\'identità valido prima di poter vendere. Sai sempre con chi stai trattando, prima di qualsiasi pagamento.' },
            { icon: Lock, title: 'Pagamenti protetti con Stripe', text: 'Il checkout più sicuro d\'Europa. I tuoi dati bancari non vengono mai condivisi con il venditore. Il pagamento viene rilasciato solo a servizio completato.', delay: '0.1s' },
            { icon: MessageCircle, title: 'Supporto e tutela acquirente', text: 'In caso di problemi, il team Eably è dalla tua parte. Sistema di segnalazione, rimborso garantito se il servizio non viene erogato. Rischio zero.', delay: '0.2s' },
          ].map(({ icon: Icon, title, text, delay }, i) => (
            <div key={i} className="cl-trust-card cl-reveal" style={delay ? { transitionDelay: delay } : {}}>
              <div className="cl-tc-icon"><Icon className="h-7 w-7" strokeWidth={1.5} /></div>
              <h3>{title}</h3>
              <p>{text}</p>
            </div>
          ))}
        </div>
      </section>

      {/* FAQ: accordion — only one item can be open at a time (controlled by openFaq index). */}
      <section className="cl-faq-section" id="faq">
        <div className="cl-faq-section-label cl-reveal">Domande frequenti</div>
        <h2 className="cl-reveal" style={{ textAlign: 'center' }}>Hai dubbi? <em>Risposta immediata.</em></h2>
        <div className="cl-faq-list">
          {FAQS.map((faq, i) => (
            <div key={i} className={`cl-faq-item cl-reveal${openFaq === i ? ' cl-open' : ''}`}>
              <button
                className="cl-faq-q"
                aria-expanded={openFaq === i}
                onClick={() => toggleFaq(i)}
              >
                {faq.q}
                <span className="cl-faq-chevron">
                  <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                    <path d="M2 4l4 4 4-4" stroke="#5B4EE8" strokeWidth="1.5" strokeLinecap="round"/>
                  </svg>
                </span>
              </button>
              {/* The cl-open class triggers the CSS height/opacity transition for the answer panel. */}
              <div className={`cl-faq-a${openFaq === i ? ' cl-open' : ''}`} role="region">
                <div className="cl-faq-a-inner">{faq.a}</div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* CTA */}
      <section className="cl-cta-section">
        <div className="cl-cta-bg-orb cl-cta-bg-1" />
        <div className="cl-cta-bg-orb cl-cta-bg-2" />
        <div className="cl-cta-inner">
          <div className="cl-cta-section-label">Inizia adesso, è gratuito</div>
          <h2>Trova il servizio giusto.<br /><em>Sostieni chi studia.</em></h2>
          <p className="cl-sub">Zero registrazione obbligatoria per sfogliare. Naviga i profili, scopri i servizi, e — solo quando sei pronto — prenota in totale sicurezza.</p>
          <Link to="/browse" className="cl-btn-primary" style={{ fontSize: '18px', padding: '1.2rem 2.5rem', display: 'inline-flex', margin: '0 auto' }}>
            Esplora i servizi disponibili
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
              <path d="M3 9h12M10 4l5 5-5 5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </Link>
          <div className="cl-cta-chips">
            {['Navigazione gratuita', 'Profili 100% verificati', 'Pagamenti sicuri Stripe', 'Zero app da installare'].map((label, i) => (
              <span key={i} className="cl-cta-chip">
                <span className="cl-chip-check">
                  <svg width="8" height="8" viewBox="0 0 8 8" fill="none">
                    <path d="M1.5 4l2 2 3-3" stroke="#1DB877" strokeWidth="1.3" strokeLinecap="round"/>
                  </svg>
                </span>
                {label}
              </span>
            ))}
          </div>
        </div>
      </section>

      {/* FOOTER */}
      <footer className="cl-footer">
        <div className="cl-footer-links">
          <a href="#">Privacy Policy</a>&nbsp;·&nbsp;
          <a href="#">Termini di Servizio</a>&nbsp;·&nbsp;
          <a href="#">Contatti</a>&nbsp;·&nbsp;
          <Link to="/register">Sei uno studente? Guadagna con Eably <ArrowRight className="inline-block h-4 w-4 align-text-bottom" strokeWidth={1.5} /></Link>
        </div>
        <div>© 2026 Eably · Easy and Reliably · P.IVA IT00000000000</div>
      </footer>
    </div>
  )
}
