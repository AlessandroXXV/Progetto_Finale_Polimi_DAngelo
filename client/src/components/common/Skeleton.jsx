const SHAPE = {
  rect: 'rounded-xl',
  circle: 'rounded-full',
  text: 'rounded-md',
  pill: 'rounded-full',
}

export default function Skeleton({
  variant = 'rect',
  width,
  height,
  className = '',
}) {
  return (
    // Lightweight placeholder block used while cards and panels are loading.
    <div
      aria-hidden="true"
      className={`skeleton ${SHAPE[variant] ?? SHAPE.rect} ${className}`}
      style={{ width, height }}
    />
  )
}
