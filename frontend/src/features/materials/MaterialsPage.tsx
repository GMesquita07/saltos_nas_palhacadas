import { useCallback, useEffect, useState } from 'react'
import { getMaterials } from '../../services/materialService'
import type { Material } from '../../types/material'
import styles from './MaterialsPage.module.css'

export function MaterialsPage() {
  const [materials, setMaterials] = useState<Material[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [hasError, setHasError] = useState(false)

  const loadMaterials = useCallback(async () => {
    try {
      const result = await getMaterials()
      setMaterials(result)
      setHasError(false)
    } catch {
      setHasError(true)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    let isCurrent = true

    void getMaterials()
      .then((result) => {
        if (!isCurrent) return
        setMaterials(result)
        setHasError(false)
      })
      .catch(() => {
        if (isCurrent) setHasError(true)
      })
      .finally(() => {
        if (isCurrent) setIsLoading(false)
      })

    window.addEventListener('materials:changed', loadMaterials)
    return () => {
      isCurrent = false
      window.removeEventListener('materials:changed', loadMaterials)
    }
  }, [loadMaterials])

  return (
    <section className={styles.page}>
      <header className={styles.header}>
        <p className="eyebrow">Eventos</p>
        <h1>Material disponível</h1>
        <p>Equipamento disponível para preparar animações e momentos com o apoio certo.</p>
      </header>

      {isLoading ? (
        <p className={styles.feedback}>A carregar material...</p>
      ) : hasError ? (
        <p className={styles.feedback}>Não foi possível carregar a lista de material.</p>
      ) : materials.length === 0 ? (
        <p className={styles.feedback}>Ainda não existe material publicado.</p>
      ) : (
        <div className={styles.list}>
          {materials.map((material) => (
            <article className={styles.card} key={material.id}>
              <figure>
                <img src={material.imageUrl} alt={material.name} />
                <figcaption>{material.name}</figcaption>
              </figure>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
