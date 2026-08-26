import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { uploadFile } from '../../../services/apiClient'
import { createMaterial, deleteMaterial, getAdminMaterials } from '../../../services/materialService'
import type { Material } from '../../../types/material'
import styles from './MaterialManagement.module.css'

type MaterialManagementProps = {
  token: string
  onNotice: (notice: { type: 'success' | 'error'; text: string }) => void
}

type MaterialFormState = {
  name: string
  imageUrl: string
}

const emptyForm = (): MaterialFormState => ({
  name: '',
  imageUrl: '',
})

export function MaterialManagement({ token, onNotice }: MaterialManagementProps) {
  const [form, setForm] = useState<MaterialFormState>(emptyForm)
  const [materials, setMaterials] = useState<Material[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [isUploading, setIsUploading] = useState(false)

  useEffect(() => {
    let isCurrent = true

    void getAdminMaterials(token)
      .then((result) => {
        if (isCurrent) setMaterials(result)
      })
      .catch(() => {
        if (isCurrent) onNotice({ type: 'error', text: 'Não foi possível carregar os materiais.' })
      })
      .finally(() => {
        if (isCurrent) setIsLoading(false)
      })

    return () => {
      isCurrent = false
    }
  }, [onNotice, token])

  async function uploadMaterialImage(event: ChangeEvent<HTMLInputElement>) {
    const input = event.currentTarget
    const file = input.files?.[0]

    if (!file) return

    if (!file.type.startsWith('image/')) {
      onNotice({ type: 'error', text: 'Seleciona uma fotografia válida para o material.' })
      input.value = ''
      return
    }

    setIsUploading(true)
    try {
      const result = await uploadFile(file, token)
      setForm((current) => ({ ...current, imageUrl: result.url }))
      onNotice({ type: 'success', text: 'Fotografia do material carregada.' })
    } catch (error) {
      onNotice({ type: 'error', text: error instanceof Error ? error.message : 'Não foi possível carregar a fotografia.' })
    } finally {
      setIsUploading(false)
      input.value = ''
    }
  }

  async function submitMaterial(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const validationError = validateMaterial(form)
    if (validationError) {
      onNotice({ type: 'error', text: validationError })
      return
    }

    if (isSaving || isUploading) return

    setIsSaving(true)
    try {
      const created = await createMaterial({
        name: form.name.trim(),
        imageUrl: form.imageUrl.trim(),
      }, token)
      setMaterials((current) => [...current, created].sort(sortMaterials))
      setForm(emptyForm())
      window.dispatchEvent(new Event('materials:changed'))
      onNotice({ type: 'success', text: 'Material adicionado à lista pública.' })
    } catch (error) {
      onNotice({ type: 'error', text: error instanceof Error ? error.message : 'Não foi possível adicionar o material.' })
    } finally {
      setIsSaving(false)
    }
  }

  async function removeMaterial(material: Material) {
    if (!window.confirm('Apagar este material da lista pública?')) return

    setIsSaving(true)
    try {
      await deleteMaterial(material.id, token)
      setMaterials((current) => current.filter((item) => item.id !== material.id))
      window.dispatchEvent(new Event('materials:changed'))
      onNotice({ type: 'success', text: 'Material removido.' })
    } catch (error) {
      onNotice({ type: 'error', text: error instanceof Error ? error.message : 'Não foi possível apagar o material.' })
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className={styles.page}>
      <form className={styles.form} onSubmit={(event) => { void submitMaterial(event) }}>
        <div className={styles.heading}>
          <p className="eyebrow">Lista pública</p>
          <h2>Novo material</h2>
          <p>Adiciona equipamento que os clientes podem consultar antes de pedir orçamento.</p>
        </div>

        <label>
          Nome do material
          <input
            maxLength={140}
            minLength={2}
            onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
            placeholder="Máquina de fumo"
            required
            value={form.name}
          />
        </label>

        <label>
          Fotografia do material
          <input accept="image/*" disabled={isUploading} type="file" onChange={uploadMaterialImage} />
        </label>

        <label>
          URL da fotografia
          <input
            maxLength={2048}
            onChange={(event) => setForm((current) => ({ ...current, imageUrl: event.target.value }))}
            placeholder="https://..."
            required
            type="url"
            value={form.imageUrl}
          />
        </label>

        {form.imageUrl && (
          <div className={styles.preview}>
            <img src={form.imageUrl} alt={form.name || 'Pré-visualização do material'} />
            <button type="button" onClick={() => setForm((current) => ({ ...current, imageUrl: '' }))}>
              Remover fotografia
            </button>
          </div>
        )}

        <button disabled={isSaving || isUploading} type="submit">
          {isUploading ? 'A carregar fotografia...' : isSaving ? 'A guardar...' : 'Adicionar material'}
        </button>
      </form>

      <section className={styles.manage}>
        <h2>Materiais publicados</h2>
        {isLoading ? (
          <p>A carregar materiais...</p>
        ) : materials.length === 0 ? (
          <p>Ainda não existem materiais publicados.</p>
        ) : (
          <div className={styles.list}>
            {materials.map((material) => (
              <article className={styles.row} key={material.id}>
                <img src={material.imageUrl} alt={material.name} />
                <strong>{material.name}</strong>
                <button disabled={isSaving} type="button" onClick={() => { void removeMaterial(material) }}>Apagar</button>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}

function validateMaterial(form: MaterialFormState) {
  if (form.name.trim().length < 2) return 'O nome do material tem de ter pelo menos 2 caracteres.'
  if (!form.imageUrl.trim()) return 'Carrega ou indica a fotografia do material.'
  if (form.imageUrl.length > 2048) return 'A URL da fotografia é demasiado longa.'
  return null
}

function sortMaterials(first: Material, second: Material) {
  return first.displayOrder - second.displayOrder || first.name.localeCompare(second.name, 'pt-PT') || first.id - second.id
}
