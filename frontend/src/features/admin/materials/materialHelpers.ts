import type { Material } from '../../../types/material'

export function materialToEditPayload(material: Pick<Material, 'name' | 'imageUrl' | 'imagePosition' | 'imageZoom'>) {
  return {
    name: material.name.trim(),
    imageUrl: material.imageUrl.trim(),
    imagePosition: material.imagePosition || '50% 50%',
    imageZoom: material.imageZoom ?? 1,
  }
}
