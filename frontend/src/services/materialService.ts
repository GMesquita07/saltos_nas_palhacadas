import { apiClient } from './apiClient'
import { mapMaterial, type ApiMaterial } from './materialMapper'
import type { CreateMaterialInput, Material, UpdateMaterialInput } from '../types/material'

export async function getMaterials() {
  const materials = await apiClient<ApiMaterial[]>('/materials', { cache: 'no-store' })
  return materials.map(mapMaterial)
}

export async function getAdminMaterials(token: string) {
  const materials = await apiClient<ApiMaterial[]>('/admin/materials', { cache: 'no-store' }, token)
  return materials.map(mapMaterial)
}


export function createMaterial(input: CreateMaterialInput, token: string) {
  return apiClient<Material>('/admin/materials', {
    method: 'POST',
    body: JSON.stringify(input),
  }, token)
}

export function updateMaterial(id: number, input: UpdateMaterialInput, token: string) {
  return apiClient<Material>('/admin/materials/' + id, {
    method: 'PUT',
    body: JSON.stringify(input),
  }, token)
}

export function deleteMaterial(id: number, token: string) {
  return apiClient<void>('/admin/materials/' + id, { method: 'DELETE' }, token)
}

export function reorderMaterials(materialIds: number[], token: string) {
  return apiClient<Material[]>('/admin/materials/order', {
    method: 'PUT',
    body: JSON.stringify({ materialIds }),
  }, token)
}
