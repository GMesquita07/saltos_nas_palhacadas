import { apiClient } from './apiClient'
import type { CreateMaterialInput, Material } from '../types/material'

export function getMaterials() {
  return apiClient<Material[]>('/materials', { cache: 'no-store' })
}

export function getAdminMaterials(token: string) {
  return apiClient<Material[]>('/admin/materials', { cache: 'no-store' }, token)
}

export function createMaterial(input: CreateMaterialInput, token: string) {
  return apiClient<Material>('/admin/materials', {
    method: 'POST',
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
