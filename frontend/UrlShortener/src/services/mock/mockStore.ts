import type { ShortLink } from '@/features/links/types'
import { readStorage, writeStorage } from '@/lib/storage'
import { seedLinks } from './data/seedLinks'
import { seedUsers, type StoredUser } from './data/seedUsers'

const STORAGE_KEYS = {
  users: 'shortly:users',
  links: 'shortly:links',
  session: 'shortly:session',
} as const

function loadLinks(): ShortLink[] {
  const links = readStorage<ShortLink[] | null>(STORAGE_KEYS.links, null)
  if (links && links.length > 0) return links
  writeStorage(STORAGE_KEYS.links, seedLinks)
  return seedLinks.map((link) => ({ ...link }))
}

function loadUsers(): StoredUser[] {
  const users = readStorage<StoredUser[] | null>(STORAGE_KEYS.users, null)
  if (users && users.length > 0) return users
  writeStorage(STORAGE_KEYS.users, seedUsers.map((u) => ({ ...u })))
  return seedUsers.map((u) => ({ ...u }))
}

function loadSession(): { userId: string; token: string } | null {
  return readStorage<{ userId: string; token: string } | null>(STORAGE_KEYS.session, null)
}

function saveLinks(links: ShortLink[]) {
  writeStorage(STORAGE_KEYS.links, links)
}

function saveUsers(users: StoredUser[]) {
  writeStorage(STORAGE_KEYS.users, users)
}

function saveSession(session: { userId: string; token: string }) {
  writeStorage(STORAGE_KEYS.session, session)
}

function clearSession() {
  try {
    localStorage.removeItem(STORAGE_KEYS.session)
  } catch {
    // ignore
  }
}

let users = loadUsers()
let links = loadLinks()

export const mockStore = {
  getUsers: () => users,
  getLinks: () => links,
  findUserByEmail: (email: string) => users.find((u) => u.email.toLowerCase() === email.toLowerCase()),
  findUserByUsername: (username: string) => users.find((u) => u.username.toLowerCase() === username.toLowerCase()),
  addUser: (user: StoredUser) => {
    users = [...users, user]
    saveUsers(users)
  },
  findLink: (id: string) => links.find((l) => l.id === id),
  findLinkByCode: (code: string) => links.find((l) => l.shortCode === code),
  addLink: (link: ShortLink) => {
    links = [...links, link]
    saveLinks(links)
    return link
  },
  updateLink: (id: string, updater: (link: ShortLink) => ShortLink) => {
    const target = links.find((l) => l.id === id)
    if (!target) return null
    const updated = updater(target)
    links = links.map((l) => (l.id === id ? updated : l))
    saveLinks(links)
    return updated
  },
  replaceLinks: (next: ShortLink[]) => {
    links = next
    saveLinks(links)
  },
  getSession: loadSession,
  setSession: saveSession,
  clearSession,
  reset: () => {
    users = seedUsers.map((u) => ({ ...u }))
    links = seedLinks.map((l) => ({ ...l }))
    saveUsers(users)
    saveLinks(links)
    clearSession()
  },
}
