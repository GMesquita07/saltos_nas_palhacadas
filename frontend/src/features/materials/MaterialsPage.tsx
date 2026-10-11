import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { imageCropStyle } from '../../components/imageCrop'
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
        <h1>Material para eventos</h1>
        <p>Consulta o equipamento publicado para apoiar animações, festas e outros eventos.</p>
      </header>

      {isLoading ? (
        <p className={styles.feedback} role="status">A carregar material...</p>
      ) : hasError ? (
        <p className={styles.feedback} role="alert">Não foi possível carregar a lista de material.</p>
      ) : materials.length === 0 ? (
        <div className={styles.emptyState}>
          <p className="eyebrow">Disponibilidade</p>
          <h2>Sem material publicado neste momento</h2>
          <p>
            Esta página reúne o equipamento que a equipa disponibiliza publicamente para apoio aos eventos.
            A seleção é atualizada quando existem materiais disponíveis para consulta.
          </p>
          <p>
            Se estás a preparar um evento e precisas de confirmar equipamento ou necessidades específicas,
            fala connosco ou envia diretamente um pedido de agendamento.
          </p>
          <div className={styles.emptyActions}>
            <Link className={styles.primaryAction} to="/contactos">Falar connosco</Link>
            <Link className={styles.secondaryAction} to="/agendar">Agendar evento</Link>
          </div>
        </div>
      ) : (
        <div className={styles.list}>
          {materials.map((material) => (
            <article className={styles.card} key={material.id}>
              <figure>
                <img
                  src={material.imageUrl}
                  alt={material.name}
                  decoding="async"
                  loading="lazy"
                  style={imageCropStyle(material.imagePosition, material.imageZoom)}
                />
                <figcaption>{material.name}</figcaption>
              </figure>
            </article>
          ))}
        </div>
      )}

      <section className={styles.infoSection} aria-labelledby="materials-info-title">
        <div className={styles.infoIntro}>
          <p className="eyebrow">Informação útil</p>
          <h2 id="materials-info-title">Como funciona o material para eventos</h2>
          <p>
            A disponibilidade pode variar consoante o evento e a data. A informação desta página serve como
            referência pública e a confirmação final é feita juntamente com o pedido.
          </p>
        </div>

        <div className={styles.infoGrid}>
          <article>
            <h3>Disponibilidade atualizada</h3>
            <p>Aqui são apresentados apenas os materiais que a equipa decidiu publicar para consulta.</p>
          </article>
          <article>
            <h3>Confirmação por evento</h3>
            <p>As necessidades e a utilização do equipamento são confirmadas de acordo com cada serviço.</p>
          </article>
          <article>
            <h3>Pedido específico</h3>
            <p>Para esclarecer uma necessidade concreta, usa os contactos ou descreve-a no pedido de agendamento.</p>
          </article>
        </div>
      </section>
    </section>
  )
}
