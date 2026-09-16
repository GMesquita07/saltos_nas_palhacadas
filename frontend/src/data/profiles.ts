import type { Profile } from '../types/profile'

// Dados temporários. Serão obtidos da API quando a área de administração existir.
export const profiles: Profile[] = [
  {
    id: 'joao-tomas',
    slug: 'joao-tomas',
    name: 'João Tomás',
    role: 'DJ & Animador',
    description: 'Música, energia e uma pista cheia do início ao fim.',
  },
  {
    id: 'kidg',
    slug: 'kidg',
    name: 'DJ KidG',
    role: 'DJ & Produtor',
    description: 'Sets pensados à medida para momentos que ficam na memória.',
  },
]
