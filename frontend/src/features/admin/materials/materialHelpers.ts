import type { Material } from '../../../types/material'

export function materialToEditPayload(material: Pick<Material, 'name' | 'imageUrl'>) {
  return {
    name: material.name.trim(),
    imageUrl: material.imageUrl.trim(),
  }
}
