import { useId } from "react"
import styles from "./index.module.css"

type props = {
  name: string
  value?: string
  setInput: (data: string) => void
  onEnter?: () => void
  autoFocus?: boolean
  optional?: boolean
}

export const PopupInput = (
  {
    name,
    value,
    setInput,
    onEnter,
    autoFocus = false,
    optional = false
  } : props
) => {
  const id = useId();
  
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && onEnter) {
      e.preventDefault();
      onEnter();
    }
  };
  
  return (
    <div className={styles.inputWrapper}>
      <input 
        className={styles.input} 
        id={id} 
        value={value}
        onChange={(e) => setInput(e.target.value)}
        onKeyDown={handleKeyDown}
        autoFocus={autoFocus}
      ></input>
      <label className={styles.inputLabel} htmlFor={id}>
        {name}{optional && <span className={styles.optional}> (необязательно)</span>}
      </label>
    </div>
  )
}
