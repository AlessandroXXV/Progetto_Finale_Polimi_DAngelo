import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import './EablyLandingNew.css'

export default function EablyLandingNew() {
  const navigate = useNavigate()
  const [scrolled, setScrolled] = useState(false)

  // Shrink the navbar once the user scrolls down the hero area.
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 40)
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  // Reveal each section the first time it enters the viewport.
  useEffect(() => {
    const observer = new IntersectionObserver(
      entries => entries.forEach(e => {
        if (e.isIntersecting) {
          e.target.classList.add('ln-visible')
          observer.unobserve(e.target)
        }
      }),
      { threshold: 0.08 }
    )
    document.querySelectorAll('.ln-reveal').forEach(el => observer.observe(el))
    return () => observer.disconnect()
  }, [])

  return (
    <div className="ln-root">
      {/* Top landing navbar with primary login and registration actions. */}
      <nav className={`ln-nav${scrolled ? ' ln-nav--scrolled' : ''}`}>
        <span className="ln-logo">Eably</span>
        <div className="ln-nav-actions">
          <button className="ln-btn-ghost" onClick={() => navigate('/login')}>Accedi</button>
          <button className="ln-btn-primary" onClick={() => navigate('/register')}>Inizia gratis</button>
        </div>
      </nav>

      {/* Hero section: explains the product value proposition. */}
      <section className="ln-hero">
        <div className="ln-hero-tag">Ripetizioni universitarie</div>
        <h1 className="ln-hero-title">
          Il tutor giusto,<br />
          <em>al momento giusto.</em>
        </h1>
        <p className="ln-hero-sub">
          Eably mette in contatto studenti universitari con chi ha bisogno di una mano.
          Prezzi equi, orari flessibili, pagamenti sicuri.
        </p>
        <div className="ln-hero-cta">
          <button className="ln-btn-primary ln-btn-lg" onClick={() => navigate('/browse')}>
            Trova un tutor
          </button>
          <button className="ln-btn-outline ln-btn-lg" onClick={() => navigate('/register?role=student')}>
            Diventa tutor
          </button>
        </div>
        <div className="ln-hero-stats">
          <div className="ln-stat">
            <span className="ln-stat-num">—</span>
            <span className="ln-stat-label">Commissioni nascoste</span>
          </div>
          <div className="ln-stat-divider" />
          <div className="ln-stat">
            <span className="ln-stat-num">48h</span>
            <span className="ln-stat-label">Per trovare il tuo tutor</span>
          </div>
          <div className="ln-stat-divider" />
          <div className="ln-stat">
            <span className="ln-stat-num">100%</span>
            <span className="ln-stat-label">Pagamenti protetti</span>
          </div>
        </div>
      </section>

      <div className="ln-rule" />

      {/* Split section: one column for tutors, one for clients. */}
      <section className="ln-split ln-reveal">
        <div className="ln-split-col">
          <span className="ln-eyebrow">01 / Se studi</span>
          <h2>Trasforma le tue competenze<br />in guadagno.</h2>
          <p>Crea il tuo profilo, scegli le materie e gli orari. Ogni sessione pagata direttamente sul tuo conto.</p>
          <button className="ln-btn-text inline-flex items-center gap-1.5" onClick={() => navigate('/register?role=student')}>
            Registrati come tutor <ArrowRight className="h-4 w-4" strokeWidth={1.5} />
          </button>
        </div>
        <div className="ln-split-divider" />
        <div className="ln-split-col">
          <span className="ln-eyebrow">02 / Se hai bisogno di aiuto</span>
          <h2>Trova il tutor che<br />fa per te.</h2>
          <p>Sfoglia i profili, leggi le recensioni, prenota direttamente. Niente telefonate, niente attese.</p>
          <button className="ln-btn-text inline-flex items-center gap-1.5" onClick={() => navigate('/browse')}>
            Cerca un tutor <ArrowRight className="h-4 w-4" strokeWidth={1.5} />
          </button>
        </div>
      </section>

      <div className="ln-rule" />

      {/* Three-step explanation of the booking flow. */}
      <section className="ln-how ln-reveal">
        <span className="ln-eyebrow">Come funziona</span>
        <h2 className="ln-section-title">Tre passi.</h2>
        <div className="ln-steps">
          <div className="ln-step">
            <span className="ln-step-num">1</span>
            <div>
              <h3>Cerca</h3>
              <p>Filtra per materia, università, prezzo e disponibilità.</p>
            </div>
          </div>
          <div className="ln-step">
            <span className="ln-step-num">2</span>
            <div>
              <h3>Prenota</h3>
              <p>Scegli il giorno e l'ora direttamente nel calendario del tutor.</p>
            </div>
          </div>
          <div className="ln-step">
            <span className="ln-step-num">3</span>
            <div>
              <h3>Impara</h3>
              <p>La sessione avviene, il pagamento viene processato in automatico.</p>
            </div>
          </div>
        </div>
      </section>

      <div className="ln-rule" />

      {/* Big-number summary to make the offer easier to scan. */}
      <section className="ln-numbers ln-reveal">
        <div className="ln-num-item">
          <span className="ln-big-num">€0</span>
          <p>Costo di registrazione</p>
        </div>
        <div className="ln-num-item">
          <span className="ln-big-num">~15€</span>
          <p>Risparmio medio all'ora<br />rispetto ai centri privati</p>
        </div>
        <div className="ln-num-item">
          <span className="ln-big-num">2 min</span>
          <p>Per creare il profilo<br />e iniziare subito</p>
        </div>
      </section>

      <div className="ln-rule" />

      {/* Final call to action before the footer. */}
      <section className="ln-cta ln-reveal">
        <h2 className="ln-cta-title">
          Inizia oggi.<br />
          <em>È gratis.</em>
        </h2>
        <div className="ln-cta-actions">
          <button className="ln-btn-primary ln-btn-lg" onClick={() => navigate('/register')}>
            Crea account
          </button>
          <button className="ln-btn-ghost" onClick={() => navigate('/browse')}>
            Oppure sfoglia i profili
          </button>
        </div>
      </section>

      {/* Minimal footer with the main navigation shortcuts. */}
      <footer className="ln-footer">
        <span className="ln-logo">Eably</span>
        <div className="ln-footer-links">
          <button onClick={() => navigate('/login')}>Accedi</button>
          <button onClick={() => navigate('/register')}>Registrati</button>
          <button onClick={() => navigate('/browse')}>Esplora</button>
        </div>
        <span className="ln-footer-copy">© 2025 Eably</span>
      </footer>
    </div>
  )
}
