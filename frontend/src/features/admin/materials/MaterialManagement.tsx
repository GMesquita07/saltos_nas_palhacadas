import { useEffect, useId, useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { useAccessibleDialog } from '../../../accessibility/useAccessibleDialog'
import { preferredScrollBehavior } from '../../../accessibility/motion'
import { ImageCropEditor } from '../../../components/ImageCropEditor'
import { defaultImageCrop, formatImagePosition, imageCropStyle, parseImageCrop, type ImageCrop } from '../../../components/imageCrop'
import { uploadFile } from '../../../services/apiClient'
import { createMaterial, deleteMaterial, getAdminMaterials, reorderMaterials, updateMaterial } from '../../../services/materialService'
import type { Material } from '../../../types/material'
import { materialToEditPayload } from './materialHelpers'
import styles from './MaterialManagement.module.css'

type MaterialManagementProps = {
  token: string
  onNotice: (notice: { type: 'success' | 'error'; text: string }) => void
}

type MaterialFormState = {
  name: string
  imageUrl: string
  imageCrop: ImageCrop
}

const emptyForm = (): MaterialFormState => ({
  name: '',
  imageUrl: '',
  imageCrop: { ...defaultImageCrop },
})

export function MaterialManagement({ token, onNotice }: MaterialManagementProps) {
  const [form, setForm] = useState<MaterialFormState>(emptyForm)
  const [materials, setMaterials] = useState<Material[]>([])
  const [editingMaterialId, setEditingMaterialId] = useState<number | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [isUploading, setIsUploading] = useState(false)
  const [isCropDialogOpen, setIsCropDialogOpen] = useState(false)

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
      setForm((current) => ({ ...current, imageUrl: result.url, imageCrop: { ...defaultImageCrop } }))
      setIsCropDialogOpen(true)
      onNotice({ type: 'success', text: 'Fotografia do material carregada. Ajusta a posição e atualiza o material.' })
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
      const payload = materialToEditPayload({
        name: form.name,
        imageUrl: form.imageUrl,
        imagePosition: formatImagePosition(form.imageCrop),
        imageZoom: form.imageCrop.zoom,
      })
      if (editingMaterialId !== null) {
        const updated = await updateMaterial(editingMaterialId, payload, token)
        setMaterials((current) => current.map((item) => item.id === updated.id ? updated : item).sort(sortMaterials))
        onNotice({ type: 'success', text: 'Material atualizado.' })
      } else {
        const created = await createMaterial(payload, token)
        setMaterials((current) => [...current, created].sort(sortMaterials))
        onNotice({ type: 'success', text: 'Material adicionado à lista pública.' })
      }
      cancelEditing()
      window.dispatchEvent(new Event('materials:changed'))
    } catch (error) {
      onNotice({ type: 'error', text: error instanceof Error ? error.message : 'Não foi possível guardar o material.' })
    } finally {
      setIsSaving(false)
    }
  }

  function startEditing(material: Material) {
    setEditingMaterialId(material.id)
    setIsCropDialogOpen(false)
    setForm({
      name: material.name,
      imageUrl: material.imageUrl,
      imageCrop: parseImageCrop(material.imagePosition, material.imageZoom),
    })
    onNotice({ type: 'success', text: 'A editar o material ' + material.name + '.' })
    window.requestAnimationFrame(() => {
      document.getElementById('material-editor')?.scrollIntoView({ behavior: preferredScrollBehavior(), block: 'start' })
    })
  }

  function cancelEditing() {
    setEditingMaterialId(null)
    setIsCropDialogOpen(false)
    setForm(emptyForm())
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

  async function reorderMaterialStack(materialIds: number[]) {
    if (isSaving || isUploading) return

    setIsSaving(true)
    try {
      const orderedMaterials = await reorderMaterials(materialIds, token)
      setMaterials(orderedMaterials)
      window.dispatchEvent(new Event('materials:changed'))
      onNotice({ type: 'success', text: 'Ordem dos materiais atualizada.' })
    } catch (error) {
      onNotice({ type: 'error', text: error instanceof Error ? error.message : 'Não foi possível guardar a ordem dos materiais.' })
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className={styles.page}>
      <form id="material-editor" className={styles.form} onSubmit={(event) => { void submitMaterial(event) }}>
        <div className={styles.heading}>
          <p className="eyebrow">Lista pública</p>
          <h2>{editingMaterialId === null ? 'Novo material' : 'Editar material'}</h2>
          <p>{editingMaterialId === null
            ? 'Adiciona equipamento que os clientes podem consultar antes de pedir orçamento.'
            : 'Atualiza o nome ou a fotografia sem alterar a posição na lista.'}</p>
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

        <label className={styles.fileUploadField}>
          <span>Fotografia do material</span>
          <span className={styles.fileUploadButton}>
            <span aria-hidden="true">＋</span>
            {isUploading ? 'A carregar fotografia...' : 'Adicionar fotografia'}
            <input
              accept="image/*"
              aria-label="Adicionar fotografia do material"
              disabled={isUploading}
              type="file"
              onChange={uploadMaterialImage}
            />
          </span>
        </label>

        <label>
          URL da fotografia
          <input
            maxLength={2048}
            onChange={(event) => {
              const imageUrl = event.target.value
              setForm((current) => ({
                ...current,
                imageUrl,
                imageCrop: imageUrl === current.imageUrl ? current.imageCrop : { ...defaultImageCrop },
              }))
            }}
            placeholder="https://..."
            required
            type="url"
            value={form.imageUrl}
          />
        </label>

        {form.imageUrl && (
          <div className={styles.preview}>
            <span className={styles.previewImageFrame}>
              <img
                src={form.imageUrl}
                alt={form.name || 'Pré-visualização do material'}
                style={imageCropStyle(formatImagePosition(form.imageCrop), form.imageCrop.zoom)}
              />
            </span>
            <div className={styles.previewActions}>
              <button className={styles.adjustImageButton} type="button" onClick={() => setIsCropDialogOpen(true)}>
                Ajustar posição
              </button>
              <button type="button" onClick={() => setForm((current) => ({ ...current, imageUrl: '', imageCrop: { ...defaultImageCrop } }))}>
                Remover fotografia
              </button>
            </div>
          </div>
        )}

        {isCropDialogOpen && form.imageUrl && (
          <MaterialImageCropDialog
            crop={form.imageCrop}
            src={form.imageUrl}
            onClose={() => setIsCropDialogOpen(false)}
            onSave={(imageCrop) => setForm((current) => ({ ...current, imageCrop }))}
          />
        )}

        <div className={styles.formActions}>
          <button disabled={isSaving || isUploading} type="submit">
            {isUploading ? 'A carregar fotografia...' : isSaving ? 'A guardar...' : editingMaterialId === null ? 'Adicionar material' : 'Atualizar material'}
          </button>
          {editingMaterialId !== null && (
            <button disabled={isSaving || isUploading} type="button" onClick={cancelEditing}>
              Cancelar edição
            </button>
          )}
        </div>
      </form>

      <section className={styles.manage}>
        <h2>Materiais publicados</h2>
        {isLoading ? (
          <p role="status">A carregar materiais...</p>
        ) : materials.length === 0 ? (
          <p>Ainda não existem materiais publicados.</p>
        ) : (
          <MaterialOrderList
            isSaving={isSaving}
            materials={materials}
            onDelete={removeMaterial}
            onEdit={startEditing}
            onReorder={reorderMaterialStack}
          />
        )}
      </section>
    </div>
  )
}

function MaterialOrderList({
  isSaving,
  materials,
  onDelete,
  onEdit,
  onReorder,
}: {
  isSaving: boolean
  materials: Material[]
  onDelete: (material: Material) => Promise<void>
  onEdit: (material: Material) => void
  onReorder: (materialIds: number[]) => Promise<void>
}) {
  const [draggingId, setDraggingId] = useState<number | null>(null)

  function dropOn(materialId: number) {
    if (draggingId === null || draggingId === materialId) return

    const draggingIndex = materials.findIndex((material) => material.id === draggingId)
    const targetIndex = materials.findIndex((material) => material.id === materialId)
    if (draggingIndex < 0 || targetIndex < 0) return

    const nextMaterials = [...materials]
    const [draggingMaterial] = nextMaterials.splice(draggingIndex, 1)
    nextMaterials.splice(targetIndex, 0, draggingMaterial)
    void onReorder(nextMaterials.map((material) => material.id))
  }

  function moveMaterial(index: number, direction: -1 | 1) {
    const targetIndex = index + direction
    if (targetIndex < 0 || targetIndex >= materials.length) return

    const nextMaterials = [...materials]
    const [material] = nextMaterials.splice(index, 1)
    nextMaterials.splice(targetIndex, 0, material)
    void onReorder(nextMaterials.map((item) => item.id))
  }

  return (
    <div className={styles.list}>
      {materials.map((material, index) => (
        <article
          className={[styles.row, draggingId === material.id ? styles.draggingRow : ''].join(' ')}
          draggable={!isSaving}
          key={material.id}
          onDragEnd={() => setDraggingId(null)}
          onDragOver={(event) => event.preventDefault()}
          onDragStart={() => setDraggingId(material.id)}
          onDrop={() => dropOn(material.id)}
        >
          <span className={styles.dragHandle} aria-hidden="true">☰</span>
          <span className={styles.rowImageFrame}>
            <img
              src={material.imageUrl}
              alt={material.name}
              style={imageCropStyle(material.imagePosition, material.imageZoom)}
            />
          </span>
          <strong>{index + 1}. {material.name}</strong>
          <div className={styles.rowActions}>
            <button aria-label={`Subir ${material.name}`} disabled={isSaving || index === 0} type="button" onClick={() => moveMaterial(index, -1)}>↑</button>
            <button aria-label={`Descer ${material.name}`} disabled={isSaving || index === materials.length - 1} type="button" onClick={() => moveMaterial(index, 1)}>↓</button>
            <button disabled={isSaving} type="button" onClick={() => onEdit(material)}>Editar</button>
            <button disabled={isSaving} type="button" onClick={() => { void onDelete(material) }}>Apagar</button>
          </div>
        </article>
      ))}
    </div>
  )
}

function MaterialImageCropDialog({
  crop,
  src,
  onClose,
  onSave,
}: {
  crop: ImageCrop
  src: string
  onClose: () => void
  onSave: (crop: ImageCrop) => void
}) {
  const [draftCrop, setDraftCrop] = useState(crop)
  const titleId = useId()
  const descriptionId = useId()
  const initialFocusRef = useRef<HTMLInputElement | null>(null)
  const dialogRef = useAccessibleDialog<HTMLDivElement>({ initialFocusRef, isOpen: true, onClose })

  function saveCrop() {
    onSave(draftCrop)
    onClose()
  }

  return (
    <div className={styles.cropDialogBackdrop} role="presentation" onMouseDown={onClose}>
      <div
        aria-describedby={descriptionId}
        aria-labelledby={titleId}
        aria-modal="true"
        className={styles.cropDialog}
        ref={dialogRef}
        role="dialog"
        tabIndex={-1}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <ImageCropEditor
          aspectRatio="16 / 10"
          comparisonPreviews={[
            { title: 'Desktop 16:10', aspectRatio: '16 / 10' },
            { title: 'Mobile 4:3', aspectRatio: '4 / 3' },
          ]}
          crop={draftCrop}
          description="Arrasta a fotografia e ajusta o zoom. A mesma posição é aplicada ao cartão desktop e mobile."
          descriptionId={descriptionId}
          initialFocusRef={initialFocusRef}
          shape="landscape"
          src={src}
          title="Ajustar fotografia do material"
          titleId={titleId}
          onChange={setDraftCrop}
        />
        <div className={styles.cropDialogActions}>
          <button type="button" onClick={saveCrop}>Guardar enquadramento</button>
          <button className={styles.cropDialogCancel} type="button" onClick={onClose}>Cancelar</button>
        </div>
      </div>
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
