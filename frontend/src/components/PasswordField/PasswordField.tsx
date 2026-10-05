import { useState } from 'react'
import styles from './PasswordField.module.css'

type PasswordFieldProps = {
  autoComplete: string
  disabled?: boolean
  hint?: string
  id: string
  label: string
  minLength?: number
  onChange: (value: string) => void
  required?: boolean
  value: string
}

export function PasswordField({
  autoComplete,
  disabled = false,
  hint,
  id,
  label,
  minLength,
  onChange,
  required = false,
  value,
}: PasswordFieldProps) {
  const [isVisible, setIsVisible] = useState(false)

  return (
    <div className={styles.field}>
      <label htmlFor={id}>{label}</label>
      <div className={styles.control}>
        <input
          autoComplete={autoComplete}
          disabled={disabled}
          id={id}
          minLength={minLength}
          onChange={(event) => onChange(event.target.value)}
          required={required}
          type={isVisible ? 'text' : 'password'}
          value={value}
        />
        <button
          aria-label={isVisible ? 'Ocultar palavra-passe' : 'Mostrar palavra-passe'}
          aria-pressed={isVisible}
          className={styles.toggle}
          disabled={disabled}
          type="button"
          onClick={() => setIsVisible((visible) => !visible)}
        >
          {isVisible ? <EyeOffIcon /> : <EyeIcon />}
        </button>
      </div>
      {hint && <small>{hint}</small>}
    </div>
  )
}

function EyeIcon() {
  return (
    <svg aria-hidden="true" focusable="false" viewBox="0 0 24 24">
      <path d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z" />
      <circle cx="12" cy="12" r="2.75" />
    </svg>
  )
}

function EyeOffIcon() {
  return (
    <svg aria-hidden="true" focusable="false" viewBox="0 0 24 24">
      <path d="M3 3l18 18" />
      <path d="M10.7 6.1A10.7 10.7 0 0 1 12 6c6 0 9.5 6 9.5 6a17.2 17.2 0 0 1-2.1 2.8M6.4 6.4A17.4 17.4 0 0 0 2.5 12s3.5 6 9.5 6a10.9 10.9 0 0 0 4.1-.8M9.9 9.9a3 3 0 0 0 4.2 4.2" />
    </svg>
  )
}
