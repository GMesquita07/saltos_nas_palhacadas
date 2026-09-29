import type { Material } from '../types/material'

export type ApiMaterial = Omit<Material, 'imagePosition' | 'imageZoom'> & {
  imagePosition?: string | null
  imageZoom?: number | null
}

export function mapMaterial(material: ApiMaterial): Material {
  return {
    ...material,
    imagePosition: material.imagePosition ?? '50% 50%',
    imageZoom: material.imageZoom ?? 1,
  }
}
