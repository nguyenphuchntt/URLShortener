import type { ShortLink } from '@/features/links/types'

const destinations = [
  ['github', 'https://github.com/shortly-team/redirect-service'],
  ['docs', 'https://docs.shortly.dev/guides/launch-checklist'],
  ['launch', 'https://www.producthunt.com/posts/shortly-links'],
  ['blog', 'https://medium.com/@shortly/building-better-links'],
  ['api', 'https://developer.mozilla.org/en-US/docs/Web/API/URL'],
  ['design', 'https://www.figma.com/community/file/shortly-brand-kit'],
  ['report', 'https://analytics.example.com/reports/q2-performance'],
  ['learn', 'https://www.coursera.org/learn/product-analytics'],
]

const aliases = ['north-star', 'api-guide', 'launch-day', 'team-notes', 'mdn-url', 'brand-kit', 'q2-report', 'learn-more', 'roadmap', 'customer-story', 'release-24', 'webinar', 'changelog', 'remote-work', 'case-study', 'onboarding', 'pricing', 'newsletter', 'community', 'security', 'status-page', 'open-source', 'mobile-app', 'press-kit', 'partners', 'demo-video', 'resources', 'handbook', 'careers', 'contact']

export const seedLinks: ShortLink[] = Array.from({ length: 30 }, (_, index) => {
  const [label, destination] = destinations[index % destinations.length] ?? ['link', 'https://shortly.dev']
  const day = String((index % 27) + 1).padStart(2, '0')
  const createdAt = `2026-07-${day}T${String(8 + (index % 10)).padStart(2, '0')}:30:00.000Z`
  const expiresAt = index === 4 ? '2026-08-01T00:00:00.000Z' : index === 17 ? '2026-08-10T00:00:00.000Z' : index === 25 ? '2026-09-30T00:00:00.000Z' : null
  const status = index === 7 || index === 19 || index === 27 ? 'DISABLED' : index === 12 || index === 23 ? 'DELETED' : 'ACTIVE'
  return {
    id: `link_${String(index + 1).padStart(3, '0')}`,
    shortCode: aliases[index] ?? `${label}-${index + 1}`,
    shortUrl: `https://sho.rt/${aliases[index] ?? `${label}-${index + 1}`}`,
    originalUrl: destination,
    status,
    clicks: [1240, 842, 316, 1890, 74, 451, 120, 2680, 92, 731][index % 10] ?? 0,
    createdAt,
    updatedAt: createdAt,
    expiresAt,
  }
})
