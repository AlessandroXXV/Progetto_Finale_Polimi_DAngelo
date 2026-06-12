// Hook that resolves a user's profile image to a Blob object URL, with module-level caching.
// Two parallel Maps are kept: one for the object URL and one for the request key that produced it,
// so a cached URL is only reused when the key (refreshKey + profileImageRevision) matches exactly.
import { useEffect, useState } from 'react'
import { userService } from '../services/userService'
import useAuthStore from '../store/authStore'

const profileImageUrlCache = new Map()
const profileImageRequestKeyCache = new Map()

// Revokes and removes cached object URL(s) to free browser memory.
// Called with no argument (or null/undefined) to clear the entire cache.
export function invalidateProfileImageCache(userId) {
  if (userId == null) {
    for (const url of profileImageUrlCache.values()) {
      URL.revokeObjectURL(url)
    }
    profileImageUrlCache.clear()
    profileImageRequestKeyCache.clear()
    return
  }

  const cachedUrl = profileImageUrlCache.get(userId)
  if (cachedUrl) {
    URL.revokeObjectURL(cachedUrl)
    profileImageUrlCache.delete(userId)
  }
  profileImageRequestKeyCache.delete(userId)
}

// Fetches the profile image for userId only when the requestKey differs from the cached one,
// then converts the raw Blob response to a stable object URL stored in the module-level cache.
async function fetchProfileImageUrl(userId, requestKey) {
  if (!userId) return null

  const cachedRequestKey = profileImageRequestKeyCache.get(userId)
  const cachedUrl = profileImageUrlCache.get(userId)
  // Skip the network request if we already have a valid URL for this exact key.
  if (cachedUrl && cachedRequestKey === requestKey) return cachedUrl

  try {
    const { data } = await userService.getProfileImage(userId, requestKey)
    // Revoke the old object URL before creating a new one to avoid memory leaks.
    if (cachedUrl) {
      URL.revokeObjectURL(cachedUrl)
    }
    const objectUrl = URL.createObjectURL(data)
    profileImageUrlCache.set(userId, objectUrl)
    profileImageRequestKeyCache.set(userId, requestKey)
    return objectUrl
  } catch {
    return null
  }
}

// refreshKey: caller-controlled version counter (e.g. after uploading a new image).
// profileImageRevision: store-level counter incremented by bumpProfileImageRevision,
// which handles invalidation triggered from other parts of the app.
export default function useProfileImage(userId, refreshKey = 0) {
  const [imageUrl, setImageUrl] = useState(null)
  const profileImageRevision = useAuthStore((state) => state.profileImageRevision)

  useEffect(() => {
    let active = true
    // Combine both version signals into a single cache key.
    const requestKey = `${refreshKey}-${profileImageRevision}`

    if (!userId) {
      return () => {
        active = false
      }
    }

    fetchProfileImageUrl(userId, requestKey).then((url) => {
      // Guard against setting state on an unmounted component.
      if (active) setImageUrl(url)
    })

    return () => {
      active = false
    }
  }, [userId, refreshKey, profileImageRevision])

  return imageUrl
}

