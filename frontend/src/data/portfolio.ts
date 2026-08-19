import type { PortfolioItem } from '../types/portfolio'

// Dados temporários. A futura API permitirá ao administrador definir data, local,
// título, tipo e URL do conteúdo de cada evento.
export const portfolioItems: PortfolioItem[] = [
  { id: 'noite-verao', type: 'Vídeo', title: 'Noite de Verão', location: 'Quinta do Lago', eventDate: '15 junho 2026' },
  { id: 'aniversario', type: 'Foto', title: 'Festa de Aniversário', location: 'Lisboa', eventDate: '3 maio 2026' },
  { id: 'casamento', type: 'Vídeo', title: 'Casamento da Sofia & Miguel', location: 'Sintra', eventDate: '20 abril 2026' },
  { id: 'festival', type: 'Foto', title: 'Festival da Alegria', location: 'Cascais', eventDate: '12 abril 2026' },
  { id: 'finalistas', type: 'Vídeo', title: 'Baile de Finalistas', location: 'Almada', eventDate: '29 março 2026' },
  { id: 'rua', type: 'Foto', title: 'Animação de Rua', location: 'Setúbal', eventDate: '9 março 2026' },
]
