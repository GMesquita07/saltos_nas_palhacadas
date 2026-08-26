export type Material = {
  id: number
  name: string
  imageUrl: string
  displayOrder: number
}

export type CreateMaterialInput = {
  name: string
  imageUrl: string
}
