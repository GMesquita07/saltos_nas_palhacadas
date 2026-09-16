import type { PortfolioItem } from './portfolio'

export type Favorite = {
  portfolioItemId: string
  profileSlug: string
  item: PortfolioItem
}
