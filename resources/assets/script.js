function applyData(e, json) {
	if (json.icon) {
		e.innerHTML = `<img src="${json.icon}?size=24" loading="lazy">${json.display_name}`
	} else {
		e.innerText = json.display_name
	}

	if (json.color) {
		e.style.color = '#' + json.color
	}

	if (json.removed) {
		e.style.color = 'white'
		e.style['text-decoration'] = 'line-through'
	}
}

const lookupObserver = new IntersectionObserver((entries, observer) => {
	entries.forEach(entry => {
		if (entry.isIntersecting) {
			const key = entry.target.dataset.lookup
			let val = sessionStorage.getItem(key)

			if (val === '') {
				entry.target.innerText = '⏳'
			} else if (val) {
				applyData(entry.target, JSON.parse(val))
			} else {
				entry.target.innerText = '⏳'
				sessionStorage.setItem(key, '')

				fetch('/api/lookup/' + key).then(r => r.json()).then(json => {
					sessionStorage.setItem(key, JSON.stringify(json))
					document.querySelectorAll(`[data-lookup="${key}"]`).forEach(e => applyData(e, json))
				}).catch(err => console.error(err))
			}

			observer.unobserve(entry.target);
		}
	});
});

document.querySelectorAll('[data-lookup]').forEach(e => {
	if (e.dataset.lookup) {
		lookupObserver.observe(e)
	}
})
