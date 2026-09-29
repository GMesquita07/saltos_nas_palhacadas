export type Material = {
  id: number
  name: string
  imageUrl: string
  imagePosition: string
  imageZoom: number
  displayOrder: number
}

export type CreateMaterialInput = {
  name: string
  imageUrl: string
  imagePosition: string
  imageZoom: number
}

export type UpdateMaterialInput = CreateMaterialInput
