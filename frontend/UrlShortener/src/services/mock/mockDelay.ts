export function mockDelay(min = 220, max = 480) {
  const duration = min + Math.floor(Math.random() * (max - min + 1))
  return new Promise<void>((resolve) => window.setTimeout(resolve, duration))
}
